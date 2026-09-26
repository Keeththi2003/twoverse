package app.twoverse.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.AuthRepository
import app.twoverse.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val state = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = state.asStateFlow()

    fun onEmailChange(email: String) {
        state.update { it.copy(email = email.trim(), message = null) }
    }

    fun onPasswordChange(password: String) {
        state.update { it.copy(password = password, message = null) }
    }

    fun onTogglePasswordVisibility() {
        state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onGoogleSignIn() {
        signIn { authRepository.signInWithGoogle() }
    }

    fun onEmailSignIn() {
        val current = state.value
        if (!current.canSubmit) return
        signIn { authRepository.signInWithEmail(current.email, current.password) }
    }

    fun onForgotPassword() {
        val email = state.value.email
        if (email.isBlank()) {
            state.update { it.copy(message = SignInMessage.ResetNeedsEmail) }
            return
        }
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            state.update {
                it.copy(message = if (result.isSuccess) SignInMessage.ResetSent else SignInMessage.SignInFailed)
            }
        }
    }

    private fun signIn(request: suspend () -> Result<UserProfile>) {
        if (state.value.isLoading) return
        state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = request()
            state.update {
                it.copy(
                    isLoading = false,
                    isSignedIn = result.isSuccess,
                    message = if (result.isSuccess) null else SignInMessage.SignInFailed,
                )
            }
        }
    }
}
