package com.example.pawalert.ui.feed

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pawalert.data.DogReport
import com.example.pawalert.data.ProblemType
import com.example.pawalert.data.ReportRepository
import com.example.pawalert.data.ReportStatus
import com.example.pawalert.util.LocationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReportWithDistance(
    val report: DogReport,
    val distanceMeters: Float?,
    val formattedDistance: String
)

data class FeedUiState(
    val isLoading: Boolean = true,
    val userLocation: Location? = null,
    val selectedCategory: ProblemType? = null,
    val selectedStatus: ReportStatus? = null,
    val reports: List<ReportWithDistance> = emptyList(),
    val error: String? = null
)

class FeedViewModel(
    application: Application,
    private val repository: ReportRepository = ReportRepository()
) : AndroidViewModel(application) {

    private val _userLocation = MutableStateFlow<Location?>(null)
    private val _selectedCategory = MutableStateFlow<ProblemType?>(null)
    private val _selectedStatus = MutableStateFlow<ReportStatus?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FeedUiState> = combine(
        repository.observeActiveReports(),
        _userLocation,
        _selectedCategory,
        _selectedStatus,
        _isLoading,
        _error
    ) { rawReports, location, category, status, loading, error ->
        val mappedReports = rawReports.map { report ->
            val distance = if (location != null && report.location.latitude != 0.0 && report.location.longitude != 0.0) {
                LocationHelper.calculateDistanceMeters(
                    startLat = location.latitude,
                    startLng = location.longitude,
                    endLat = report.location.latitude,
                    endLng = report.location.longitude
                )
            } else null

            ReportWithDistance(
                report = report,
                distanceMeters = distance,
                formattedDistance = LocationHelper.formatDistance(distance)
            )
        }

        // Filter by category and status
        val filtered = mappedReports.filter { item ->
            val matchesCategory = category == null || item.report.problemTypeEnum() == category
            val matchesStatus = status == null || item.report.statusEnum() == status
            matchesCategory && matchesStatus
        }

        // Sort by distance (closest first), fallback to newest first if distance unknown
        val sorted = filtered.sortedWith(
            compareBy<ReportWithDistance> { it.distanceMeters ?: Float.MAX_VALUE }
                .thenByDescending { it.report.createdAt }
        )

        FeedUiState(
            isLoading = loading,
            userLocation = location,
            selectedCategory = category,
            selectedStatus = status,
            reports = sorted,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeedUiState()
    )

    init {
        refreshLocation()
        viewModelScope.launch {
            // Wait for first emission from flow then stop initial loading indicator
            repository.observeActiveReports().collect {
                _isLoading.value = false
            }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val location = LocationHelper.getCurrentLocation(getApplication())
            _userLocation.value = location
        }
    }

    fun selectCategory(category: ProblemType?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun selectStatus(status: ReportStatus?) {
        _selectedStatus.value = if (_selectedStatus.value == status) null else status
    }
}
