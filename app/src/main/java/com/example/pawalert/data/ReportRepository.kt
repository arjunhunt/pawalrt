package com.example.pawalert.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
     * Safely reads, rotates, downsamples, and compresses the photo into a Base64 data URI.
     * Prevents OOM crashes from high-resolution phone cameras.
     */
    suspend fun processPhoto(context: Context, localUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            // 1. Decode bounds only to prevent loading huge bitmap into RAM
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(localUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext ""

            // 2. Calculate inSampleSize targeting ~800px max
            val maxTarget = 800
            var sampleSize = 1
            while (origWidth / (sampleSize * 2) >= maxTarget && origHeight / (sampleSize * 2) >= maxTarget) {
                sampleSize *= 2
            }

            // 3. Decode scaled bitmap using RGB_565 (50% less RAM)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decodedBitmap = context.contentResolver.openInputStream(localUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext ""

            // 4. Read EXIF orientation to correct camera rotation
            var rotationDegrees = 0f
            try {
                context.contentResolver.openInputStream(localUri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    rotationDegrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                }
            } catch (_: Throwable) {}

            val matrix = Matrix()
            if (rotationDegrees != 0f) {
                matrix.postRotate(rotationDegrees)
            }

            // Scale to final target max dimension
            val currentMax = maxOf(decodedBitmap.width, decodedBitmap.height)
            if (currentMax > maxTarget) {
                val scale = maxTarget.toFloat() / currentMax
                matrix.postScale(scale, scale)
            }

            val finalBitmap = if (!matrix.isIdentity) {
                Bitmap.createBitmap(
                    decodedBitmap,
                    0,
                    0,
                    decodedBitmap.width,
                    decodedBitmap.height,
                    matrix,
                    true
                )
            } else {
                decodedBitmap
            }

            // 5. Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
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
        address: String,
        landmark: String = ""
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
            landmark = landmark,
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
