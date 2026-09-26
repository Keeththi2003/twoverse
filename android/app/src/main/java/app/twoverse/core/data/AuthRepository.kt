package app.twoverse.core.data

import app.twoverse.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** The signed-in user, or null when signed out. */
    val currentUser: Flow<UserProfile?>

    suspend fun signInWithGoogle(): Result<UserProfile>

    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>

    /** Sends a password-reset email (FR-AUTH-3). */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    suspend fun signOut()

    /** Asks the backend to delete the account and all its data, then signs out (FR-SET-6). */
    suspend fun requestAccountDeletion()
}
