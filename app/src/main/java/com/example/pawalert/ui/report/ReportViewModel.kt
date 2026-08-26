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
    application: Application
) : AndroidViewModel(application) {

    private val repository = ReportRepository()

    private val _uiState = MutableStateFlow(ReportFormState())
    val uiState: StateFlow<ReportFormState> = _uiState.asStateFlow()

    fun onPhotoSelected(uri: Uri) {
        _uiState.update { it.copy(selectedPhotoUri = uri) }
    }

    fun prepareCameraUri(): Uri {
        val uri = LocationHelper.createTempImageUri(getApplication())
        _uiState.update { it.copy(tempCameraUri = uri) }
        return uri
    }

    fun onCameraPhotoCaptured() {
        val uri = _uiState.value.tempCameraUri ?: return
        _uiState.update { it.copy(selectedPhotoUri = uri, tempCameraUri = null) }
    }

    fun onCategorySelected(category: ProblemType) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true) }
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
                _uiState.update { it.copy(isFetchingLocation = false) }
            }
        }
    }

    fun submitReport() {
        val state = _uiState.value
        val photoUri = state.selectedPhotoUri ?: return
        val lat = state.latitude ?: return
        val lng = state.longitude ?: return

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
                    address = state.address.ifBlank { "Location coordinates recorded" }
                )

                _uiState.update { it.copy(submissionState = ReportSubmissionState.Success) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(submissionState = ReportSubmissionState.Error(e.localizedMessage ?: "Submission failed. Please check network."))
                }
            }
        }
    }

    fun dismissError() {
        if (_uiState.value.submissionState is ReportSubmissionState.Error) {
            _uiState.update { it.copy(submissionState = ReportSubmissionState.Idle) }
        }
    }
}
