package app.twoverse.feature.auth

import app.twoverse.core.model.DataError

data class ResetPasswordUiState(
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: DataError? = null,
    val isSaved: Boolean = false,
) {
    val canSave: Boolean get() = password.isNotBlank() && !isLoading
}
