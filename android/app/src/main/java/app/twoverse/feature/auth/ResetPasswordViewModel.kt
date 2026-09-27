package app.twoverse.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.AuthRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** New password after opening the reset link from the email (FR-AUTH-3). */
@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val state = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = state.asStateFlow()

    fun onPasswordChange(password: String) {
        state.update { it.copy(password = password, error = null) }
    }

    fun onTogglePasswordVisibility() {
        state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onSave() {
        val current = state.value
        if (!current.canSave) return
        if (current.password.length < SignUpViewModel.MinPasswordLength) {
            state.update { it.copy(error = DataError.WeakPassword) }
            return
        }
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.updatePassword(current.password)) {
                is DataResult.Success -> state.update { it.copy(isLoading = false, isSaved = true) }
                is DataResult.Failure -> state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
