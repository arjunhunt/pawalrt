package com.example.pawalert

import com.example.pawalert.data.DogReport
import com.example.pawalert.data.ProblemType
import com.example.pawalert.data.ReportStatus
import com.example.pawalert.util.LocationHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class PawAlertModelTest {

    @Test
    fun testProblemTypeEnumFallback() {
        val report = DogReport(problemType = "UNKNOWN_CUSTOM_VALUE")
        assertEquals(ProblemType.OTHER, report.problemTypeEnum())

        val hungryReport = DogReport(problemType = ProblemType.HUNGRY.name)
        assertEquals(ProblemType.HUNGRY, hungryReport.problemTypeEnum())
    }

    @Test
    fun testReportStatusEnumFallback() {
        val report = DogReport(status = "INVALID_STATUS")
        assertEquals(ReportStatus.OPEN, report.statusEnum())

        val inProgressReport = DogReport(status = ReportStatus.IN_PROGRESS.name)
        assertEquals(ReportStatus.IN_PROGRESS, inProgressReport.statusEnum())
    }

    @Test
    fun testFormatDistance() {
        assertEquals("350 m away", LocationHelper.formatDistance(350f))
        assertEquals("1.5 km away", LocationHelper.formatDistance(1500f))
        assertEquals("12.0 km away", LocationHelper.formatDistance(12000f))
        assertEquals("Distance unknown", LocationHelper.formatDistance(null))
    }

    @Test
    fun testFormatTimeAgo() {
        val now = System.currentTimeMillis()
        assertEquals("Just now", LocationHelper.formatTimeAgo(now - 10_000))
        assertEquals("5m ago", LocationHelper.formatTimeAgo(now - 5 * 60 * 1000))
        assertEquals("2h ago", LocationHelper.formatTimeAgo(now - 2 * 60 * 60 * 1000))
    }
}
