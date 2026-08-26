package com.example.pawalert.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pawalert.data.DogReport
import com.example.pawalert.data.ReportRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DetailActionState {
    data object Idle : DetailActionState
    data object Loading : DetailActionState
    data class Error(val message: String) : DetailActionState
}

data class DetailUiState(
    val report: DogReport? = null,
    val isLoading: Boolean = true,
    val currentUserId: String = "",
    val actionState: DetailActionState = DetailActionState.Idle
) {
    val isClaimedByCurrentUser: Boolean
        get() = report?.helperId == currentUserId && currentUserId.isNotBlank()
}

class DetailViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = ReportRepository()
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _uiState = MutableStateFlow(DetailUiState(currentUserId = auth.currentUser?.uid.orEmpty()))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadReport(reportId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentUserId = auth.currentUser?.uid.orEmpty()) }
            repository.observeReport(reportId).collect { report ->
                _uiState.update {
                    it.copy(
                        report = report,
                        isLoading = false,
                        currentUserId = auth.currentUser?.uid.orEmpty()
                    )
                }
            }
        }
    }

    fun claimReport(reportId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(actionState = DetailActionState.Loading) }
                repository.claimReport(reportId)
                _uiState.update { it.copy(actionState = DetailActionState.Idle) }
            } catch (e: Exception) {
                _uiState.update { it.copy(actionState = DetailActionState.Error(e.localizedMessage ?: "Failed to claim alert.")) }
            }
        }
    }

    fun markResolved(reportId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(actionState = DetailActionState.Loading) }
                repository.markResolved(reportId)
                _uiState.update { it.copy(actionState = DetailActionState.Idle) }
            } catch (e: Exception) {
                _uiState.update { it.copy(actionState = DetailActionState.Error(e.localizedMessage ?: "Failed to mark resolved.")) }
            }
        }
    }

    fun unclaimReport(reportId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(actionState = DetailActionState.Loading) }
                repository.unclaimReport(reportId)
                _uiState.update { it.copy(actionState = DetailActionState.Idle) }
            } catch (e: Exception) {
                _uiState.update { it.copy(actionState = DetailActionState.Error(e.localizedMessage ?: "Failed to release alert.")) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(actionState = DetailActionState.Idle) }
    }
}
