package app.twoverse.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.model.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val coupleRepository: CoupleRepository,
) : ViewModel() {

    private val state = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = state.asStateFlow()

    fun onEmailChange(email: String) {
        state.update { it.copy(email = email.trim(), message = null, error = null) }
    }

    fun onPasswordChange(password: String) {
        state.update { it.copy(password = password, message = null, error = null) }
    }

    fun onTogglePasswordVisibility() {
        state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /** Credential Manager returned a Google ID token (FR-AUTH-1). */
    fun onGoogleIdToken(idToken: String, rawNonce: String) {
        signIn { authRepository.signInWithGoogle(idToken, rawNonce) }
    }

    /** Credential Manager failed for a reason other than the user closing it. */
    fun onGoogleSignInFailed() {
        state.update { it.copy(message = SignInMessage.GoogleFailed, error = null) }
    }

    fun onEmailSignIn() {
        val current = state.value
        if (!current.canSubmit) return
        signIn { authRepository.signInWithEmail(current.email, current.password) }
    }

    /** FR-AUTH-3: the email links back to the app to choose a new password. */
    fun onForgotPassword() {
        val email = state.value.email
        if (email.isBlank()) {
            state.update { it.copy(message = SignInMessage.ResetNeedsEmail, error = null) }
            return
        }
        viewModelScope.launch {
            when (val result = authRepository.sendPasswordReset(email)) {
                is DataResult.Success -> state.update { it.copy(message = SignInMessage.ResetSent, error = null) }
                is DataResult.Failure -> state.update { it.copy(message = null, error = result.error) }
            }
        }
    }

    private fun signIn(request: suspend () -> DataResult<Unit>) {
        if (state.value.isLoading) return
        state.update { it.copy(isLoading = true, message = null, error = null) }
        viewModelScope.launch {
            when (val result = request()) {
                is DataResult.Success -> {
                    val paired = coupleRepository.couple.first() != null
                    val destination = if (paired) SignedInDestination.Home else SignedInDestination.Pair
                    state.update { it.copy(isLoading = false, destination = destination) }
                }
                is DataResult.Failure -> state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
