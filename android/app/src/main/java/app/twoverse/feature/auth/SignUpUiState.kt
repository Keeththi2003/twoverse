package app.twoverse.feature.auth

import app.twoverse.core.model.DataError

data class SignUpUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val problem: SignUpProblem? = null,
    val error: DataError? = null,
    /** Email confirmation is on for the project, so the user must confirm before signing in. */
    val isConfirmationSent: Boolean = false,
    val isSignedUp: Boolean = false,
) {
    /** Any input enables the button; [SignUpViewModel] then explains what is missing. */
    val canSubmit: Boolean
        get() = displayName.isNotEmpty() && email.isNotBlank() && password.isNotEmpty() && !isLoading
}

/** Input problems caught before calling the server. */
enum class SignUpProblem { NameMissing, EmailInvalid }
