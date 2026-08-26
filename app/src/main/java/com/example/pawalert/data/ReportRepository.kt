package com.example.pawalert.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ReportRepository {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val reportsCollection by lazy { firestore.collection("reports") }

    /**
     * Compresses the dog photo to an optimized JPEG and encodes to Base64 data URI.
     * Stored directly in Firestore - eliminates the need for paid Cloud Storage plans!
     */
    suspend fun processPhoto(context: Context, localUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(localUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return@withContext ""

            // Scale to max 800px on longest dimension (~40-60 KB)
            val maxDim = 800
            val maxOriginal = maxOf(originalBitmap.width, originalBitmap.height)
            val ratio = if (maxOriginal > maxDim) maxDim.toFloat() / maxOriginal else 1.0f

            val targetWidth = (originalBitmap.width * ratio).toInt()
            val targetHeight = (originalBitmap.height * ratio).toInt()

            val scaledBitmap = if (ratio < 1.0f) {
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64String"
        } catch (e: Throwable) {
            e.printStackTrace()
            ""
        }
    }

    /** Creates a new dog-welfare report in Firestore. */
    suspend fun submitReport(
        problemType: ProblemType,
        description: String,
        photoUrl: String,
        latitude: Double,
        longitude: Double,
        address: String
    ) {
        val user = auth.currentUser
        val doc = reportsCollection.document()
        val report = DogReport(
            id = doc.id,
            reporterId = user?.uid.orEmpty(),
            reporterName = user?.displayName ?: "Anonymous",
            problemType = problemType.name,
            description = description,
            photoUrl = photoUrl,
            location = GeoPoint(latitude, longitude),
            address = address,
            status = ReportStatus.OPEN.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        doc.set(report).await()
    }

    /**
     * Real-time stream of all non-resolved reports.
     * Safely catches any Firestore errors without crashing the app.
     */
    fun observeActiveReports(): Flow<List<DogReport>> = callbackFlow<List<DogReport>> {
        try {
            val registration = reportsCollection
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        error.printStackTrace()
                        trySend(emptyList<DogReport>())
                        return@addSnapshotListener
                    }
                    try {
                        val reports = snapshot?.toObjects(DogReport::class.java) ?: emptyList<DogReport>()
                        val activeReports = reports.filter {
                            it.status == ReportStatus.OPEN.name || it.status == ReportStatus.IN_PROGRESS.name
                        }
                        trySend(activeReports)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        trySend(emptyList<DogReport>())
                    }
                }
            awaitClose { registration.remove() }
        } catch (e: Throwable) {
            e.printStackTrace()
            trySend(emptyList<DogReport>())
            close()
        }
    }.catch { e ->
        e.printStackTrace()
        emit(emptyList<DogReport>())
    }

    fun observeReport(reportId: String): Flow<DogReport?> = callbackFlow<DogReport?> {
        try {
            val registration = reportsCollection.document(reportId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        error.printStackTrace()
                        trySend(null as DogReport?)
                        return@addSnapshotListener
                    }
                    try {
                        trySend(snapshot?.toObject(DogReport::class.java))
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        trySend(null as DogReport?)
                    }
                }
            awaitClose { registration.remove() }
        } catch (e: Throwable) {
            e.printStackTrace()
            trySend(null as DogReport?)
            close()
        }
    }.catch { e ->
        e.printStackTrace()
        emit(null as DogReport?)
    }

    /** Called when a nearby feeder taps "I'll help this dog". */
    suspend fun claimReport(reportId: String) {
        val user = auth.currentUser ?: return
        reportsCollection.document(reportId).update(
            mapOf(
                "status" to ReportStatus.IN_PROGRESS.name,
                "helperId" to user.uid,
                "helperName" to (user.displayName ?: "A nearby feeder"),
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun markResolved(reportId: String) {
        reportsCollection.document(reportId).update(
            mapOf(
                "status" to ReportStatus.RESOLVED.name,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun unclaimReport(reportId: String) {
        reportsCollection.document(reportId).update(
            mapOf(
                "status" to ReportStatus.OPEN.name,
                "helperId" to null,
                "helperName" to null,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }
}
