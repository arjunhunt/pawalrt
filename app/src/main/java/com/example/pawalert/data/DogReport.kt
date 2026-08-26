package com.example.pawalert.data

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

enum class ProblemType(val label: String) {
    HUNGRY("Hungry / Needs Food"),
    INJURED("Injured"),
    SICK("Sick"),
    STUCK("Stuck / Trapped"),
    AGGRESSIVE("Aggressive Behavior"),
    LOST("Lost Dog"),
    NEWBORN_LITTER("Newborn Litter"),
    OTHER("Other")
}

enum class ReportStatus(val label: String) {
    OPEN("Open"),
    IN_PROGRESS("Being Handled"),
    RESOLVED("Resolved")
}

/**
 * Represents a single reported dog-welfare problem.
 * Stored as a document in the "reports" Firestore collection.
 */
data class DogReport(
    var id: String = "",
    var reporterId: String = "",
    var reporterName: String = "",
    var problemType: String = ProblemType.OTHER.name,
    var description: String = "",
    var photoUrl: String = "",
    var location: GeoPoint = GeoPoint(0.0, 0.0),
    var address: String = "",
    var landmark: String = "",
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = ReportStatus.OPEN.name,
    var helperId: String? = null,
    var helperName: String? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun problemTypeEnum(): ProblemType =
        runCatching { ProblemType.valueOf(problemType) }.getOrDefault(ProblemType.OTHER)

    fun statusEnum(): ReportStatus =
        runCatching { ReportStatus.valueOf(status) }.getOrDefault(ReportStatus.OPEN)
}
