package app.twoverse.core.model

sealed interface AuthState {
    /** The saved session is still being restored (FR-AUTH-4). */
    data object Loading : AuthState

    data object SignedOut : AuthState

    data class SignedIn(val userId: String) : AuthState
}

/** Email sign-up either signs in straight away or waits for email confirmation. */
enum class SignUpResult { SignedIn, ConfirmEmail }
