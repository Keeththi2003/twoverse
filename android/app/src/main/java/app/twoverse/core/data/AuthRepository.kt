package app.twoverse.core.data

import app.twoverse.core.model.AuthState
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.SignUpResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Session state; a saved session survives restarts (FR-AUTH-4). */
    val authState: Flow<AuthState>

    /** Signs in with a Google ID token from Credential Manager (FR-AUTH-1). */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String): DataResult<Unit>

    suspend fun signInWithEmail(email: String, password: String): DataResult<Unit>

    /** Email sign-up with the display name shown to the partner (FR-AUTH-2, FR-AUTH-5). */
    suspend fun signUpWithEmail(displayName: String, email: String, password: String): DataResult<SignUpResult>

    /** Sends a reset email that opens the app through the auth-callback deep link (FR-AUTH-3). */
    suspend fun sendPasswordReset(email: String): DataResult<Unit>

    /** Sets a new password for the recovery session opened from the reset email. */
    suspend fun updatePassword(newPassword: String): DataResult<Unit>

    suspend fun signOut(): DataResult<Unit>

    /** Deletes the account and all its data on the server, then signs out (FR-SET-6). */
    suspend fun deleteAccount(): DataResult<Unit>
}
