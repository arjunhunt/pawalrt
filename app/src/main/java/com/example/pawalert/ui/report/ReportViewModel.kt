package com.example.pawalert.ui.report

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pawalert.data.DogReport
import com.example.pawalert.data.ProblemType
import com.example.pawalert.data.ReportRepository
import com.example.pawalert.util.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ReportSubmissionState {
    data object Idle : ReportSubmissionState
    data object UploadingPhoto : ReportSubmissionState
    data object Submitting : ReportSubmissionState
    data object Success : ReportSubmissionState
    data class Error(val message: String) : ReportSubmissionState
}

data class ReportFormState(
    val selectedPhotoUri: Uri? = null,
    val tempCameraUri: Uri? = null,
    val selectedCategory: ProblemType = ProblemType.HUNGRY,
    val description: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "",
    val isFetchingLocation: Boolean = false,
    val submissionState: ReportSubmissionState = ReportSubmissionState.Idle
) {
    val isFormValid: Boolean
        get() = selectedPhotoUri != null &&
                description.isNotBlank() &&
                latitude != null &&
                longitude != null
}

class ReportViewModel(
    application: Application,
    private val repository: ReportRepository = ReportRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReportFormState())
    val uiState: StateFlow<ReportFormState> = _uiState.asStateFlow()

    fun onPhotoSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedPhotoUri = uri) }
    }

    fun prepareCameraUri(): Uri {
        val uri = LocationHelper.createTempImageUri(getApplication())
        _uiState.update { it.copy(tempCameraUri = uri) }
        return uri
    }

    fun onCameraPhotoCaptured() {
        val uri = _uiState.value.tempCameraUri
        if (uri != null) {
            _uiState.update { it.copy(selectedPhotoUri = uri) }
        }
    }

    fun onCategorySelected(category: ProblemType) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun fetchCurrentLocation() {
        _uiState.update { it.copy(isFetchingLocation = true) }
        viewModelScope.launch {
            val location = LocationHelper.getCurrentLocation(getApplication())
            if (location != null) {
                val address = LocationHelper.getAddressFromLocation(
                    getApplication(),
                    location.latitude,
                    location.longitude
                )
                _uiState.update {
                    it.copy(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address,
                        isFetchingLocation = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isFetchingLocation = false,
                        submissionState = ReportSubmissionState.Error("Could not determine current location. Please ensure GPS is enabled.")
                    )
                }
            }
        }
    }

    fun submitReport() {
        val state = _uiState.value
        val photoUri = state.selectedPhotoUri
        val lat = state.latitude
        val lng = state.longitude

        if (photoUri == null) {
            _uiState.update { it.copy(submissionState = ReportSubmissionState.Error("Please take or select a photo of the dog.")) }
            return
        }
        if (state.description.isBlank()) {
            _uiState.update { it.copy(submissionState = ReportSubmissionState.Error("Please provide a short description.")) }
            return
        }
        if (lat == null || lng == null) {
            _uiState.update { it.copy(submissionState = ReportSubmissionState.Error("Location is required. Please capture your location.")) }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(submissionState = ReportSubmissionState.UploadingPhoto) }
                val downloadUrl = repository.uploadPhoto(photoUri)

                _uiState.update { it.copy(submissionState = ReportSubmissionState.Submitting) }
                repository.submitReport(
                    problemType = state.selectedCategory,
                    description = state.description.trim(),
                    photoUrl = downloadUrl,
                    latitude = lat,
                    longitude = lng,
                    address = state.address.ifBlank { "Nearby" }
                )

                _uiState.update { it.copy(submissionState = ReportSubmissionState.Success) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(submissionState = ReportSubmissionState.Error(e.localizedMessage ?: "Failed to submit report. Please try again."))
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(submissionState = ReportSubmissionState.Idle) }
    }
}
