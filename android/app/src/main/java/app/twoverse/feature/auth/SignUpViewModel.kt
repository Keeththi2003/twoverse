package app.twoverse.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.AuthRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.SignUpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Email sign-up with the display name the partner sees (FR-AUTH-2, FR-AUTH-5). */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val state = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = state.asStateFlow()

    fun onDisplayNameChange(name: String) {
        state.update { it.copy(displayName = name.take(MaxNameLength), problem = null, error = null) }
    }

    fun onEmailChange(email: String) {
        state.update { it.copy(email = email.trim(), problem = null, error = null) }
    }

    fun onPasswordChange(password: String) {
        state.update { it.copy(password = password, problem = null, error = null) }
    }

    fun onTogglePasswordVisibility() {
        state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onSignUp() {
        val current = state.value
        if (!current.canSubmit) return
        val name = current.displayName.trim()
        val problem = when {
            name.isEmpty() -> SignUpProblem.NameMissing
            !EmailPattern.matches(current.email) -> SignUpProblem.EmailInvalid
            else -> null
        }
        if (problem != null || current.password.length < MinPasswordLength) {
            state.update { it.copy(problem = problem, error = if (problem == null) DataError.WeakPassword else null) }
            return
        }
        state.update { it.copy(isLoading = true, problem = null, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.signUpWithEmail(name, current.email, current.password)) {
                is DataResult.Success -> state.update {
                    it.copy(
                        isLoading = false,
                        isSignedUp = result.value == SignUpResult.SignedIn,
                        isConfirmationSent = result.value == SignUpResult.ConfirmEmail,
                    )
                }
                is DataResult.Failure -> state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    companion object {
        /** Supabase Auth's minimum password length (supabase/config.toml). */
        const val MinPasswordLength = 6

        /** Matches profiles.display_name in the database. */
        const val MaxNameLength = 50
        private val EmailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
