package com.example.pawalert.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data object Authenticated : AuthState
    data class Error(val message: String) : AuthState
}

data class AuthUiState(
    val currentUser: FirebaseUser? = null,
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    val authState: AuthState = AuthState.Idle
)

class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            currentUser = auth.currentUser,
            displayName = auth.currentUser?.displayName.orEmpty(),
            authState = if (auth.currentUser != null) AuthState.Authenticated else AuthState.Idle
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun toggleAuthMode() {
        _uiState.update { it.copy(isSignUp = !it.isSignUp) }
    }

    /**
     * Quick 1-click anonymous sign in with display name setup for community helpers.
     */
    fun continueAsCommunityFeeder(name: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(authState = AuthState.Loading) }
                val result = auth.signInAnonymously().await()
                val user = result.user

                if (user != null && name.isNotBlank()) {
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(name.trim())
                        .build()
                    user.updateProfile(profileUpdate).await()
                }

                _uiState.update {
                    it.copy(
                        currentUser = auth.currentUser,
                        authState = AuthState.Authenticated
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(authState = AuthState.Error(e.localizedMessage ?: "Failed to sign in"))
                }
            }
        }
    }

    fun authenticateWithEmail() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password.trim()
        val name = _uiState.value.displayName.trim()
        val isSignUp = _uiState.value.isSignUp

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authState = AuthState.Error("Please enter email and password")) }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(authState = AuthState.Loading) }
                if (isSignUp) {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    if (name.isNotBlank()) {
                        val profileUpdate = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        result.user?.updateProfile(profileUpdate)?.await()
                    }
                } else {
                    auth.signInWithEmailAndPassword(email, password).await()
                }

                _uiState.update {
                    it.copy(
                        currentUser = auth.currentUser,
                        authState = AuthState.Authenticated
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(authState = AuthState.Error(e.localizedMessage ?: "Authentication failed"))
                }
            }
        }
    }

    fun updateDisplayName(name: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(name.trim())
                    .build()
                user.updateProfile(profileUpdate).await()
                _uiState.update { it.copy(displayName = name.trim(), currentUser = auth.currentUser) }
            } catch (e: Exception) {
                _uiState.update { it.copy(authState = AuthState.Error(e.localizedMessage ?: "Update failed")) }
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _uiState.update {
            AuthUiState(currentUser = null, authState = AuthState.Idle)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(authState = AuthState.Idle) }
    }
}
