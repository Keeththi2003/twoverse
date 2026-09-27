package app.twoverse.feature.auth

import app.twoverse.core.model.DataError

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val message: SignInMessage? = null,
    val error: DataError? = null,
    val destination: SignedInDestination? = null,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}

enum class SignInMessage { ResetSent, ResetNeedsEmail, GoogleFailed }
