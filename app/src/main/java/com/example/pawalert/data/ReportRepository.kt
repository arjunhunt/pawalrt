package com.example.pawalert.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ReportRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val reportsCollection = firestore.collection("reports")

    /** Uploads a photo to Firebase Storage and returns its public download URL. */
    suspend fun uploadPhoto(localUri: Uri): String {
        val uid = auth.currentUser?.uid ?: "anonymous"
        val fileName = "reports/$uid/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putFile(localUri).await()
        return ref.downloadUrl.await().toString()
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
     * Real-time stream of all non-resolved reports, newest first.
     * Distance-based sorting/filtering happens client-side in the ViewModel
     * (Firestore doesn't do geo-radius queries without extra geohashing setup).
     */
    fun observeActiveReports(): Flow<List<DogReport>> = callbackFlow {
        val registration = reportsCollection
            .whereIn("status", listOf(ReportStatus.OPEN.name, ReportStatus.IN_PROGRESS.name))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(DogReport::class.java) ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeReport(reportId: String): Flow<DogReport?> = callbackFlow {
        val registration = reportsCollection.document(reportId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(DogReport::class.java))
            }
        awaitClose { registration.remove() }
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
