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

class AuthViewModel : ViewModel() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _uiState = MutableStateFlow(
        AuthUiState(
            currentUser = try { FirebaseAuth.getInstance().currentUser } catch (_: Throwable) { null },
            displayName = try { FirebaseAuth.getInstance().currentUser?.displayName.orEmpty() } catch (_: Throwable) { "" },
            authState = try {
                if (FirebaseAuth.getInstance().currentUser != null) AuthState.Authenticated else AuthState.Idle
            } catch (_: Throwable) {
                AuthState.Idle
            }
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
        _uiState.update { it.copy(isSignUp = !it.isSignUp, authState = AuthState.Idle) }
    }

    fun updateDisplayName(displayName: String) {
        val trimmed = displayName.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user != null) {
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(trimmed)
                        .build()
                    user.updateProfile(profileUpdate).await()
                    _uiState.update { it.copy(displayName = trimmed, currentUser = auth.currentUser) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(authState = AuthState.Error(e.localizedMessage ?: "Failed to update name")) }
            }
        }
    }

    fun continueAsCommunityFeeder(nickname: String) {
        if (nickname.isBlank()) {
            _uiState.update { it.copy(authState = AuthState.Error("Please enter your name or feeder nickname")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(authState = AuthState.Loading) }
            try {
                val result = auth.signInAnonymously().await()
                val user = result.user
                if (user != null && nickname.isNotBlank()) {
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(nickname.trim())
                        .build()
                    user.updateProfile(profileUpdate).await()
                }

                _uiState.update {
                    it.copy(
                        currentUser = auth.currentUser,
                        displayName = nickname.trim(),
                        authState = AuthState.Authenticated
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(authState = AuthState.Error(e.localizedMessage ?: "Failed to sign in. Please check network."))
                }
            }
        }
    }

    fun authenticateWithEmail() {
        val state = _uiState.value
        val email = state.email.trim()
        val pass = state.password.trim()

        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(authState = AuthState.Error("Please enter both email and password")) }
            return
        }

        if (pass.length < 6) {
            _uiState.update { it.copy(authState = AuthState.Error("Password must be at least 6 characters")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(authState = AuthState.Loading) }
            try {
                if (state.isSignUp) {
                    val result = auth.createUserWithEmailAndPassword(email, pass).await()
                    if (state.displayName.isNotBlank()) {
                        result.user?.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(state.displayName.trim())
                                .build()
                        )?.await()
                    }
                } else {
                    auth.signInWithEmailAndPassword(email, pass).await()
                }

                _uiState.update {
                    it.copy(
                        currentUser = auth.currentUser,
                        displayName = auth.currentUser?.displayName ?: state.displayName,
                        authState = AuthState.Authenticated
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(authState = AuthState.Error(e.localizedMessage ?: "Authentication failed."))
                }
            }
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (_: Throwable) {}
        _uiState.update {
            AuthUiState(
                currentUser = null,
                displayName = "",
                authState = AuthState.Idle
            )
        }
    }

    fun dismissError() {
        if (_uiState.value.authState is AuthState.Error) {
            _uiState.update { it.copy(authState = AuthState.Idle) }
        }
    }
}
