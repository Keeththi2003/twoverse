package app.twoverse.feature.auth

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val message: SignInMessage? = null,
    val isSignedIn: Boolean = false,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}

enum class SignInMessage { ResetSent, ResetNeedsEmail, SignInFailed }
