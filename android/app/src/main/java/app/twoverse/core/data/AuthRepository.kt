package app.twoverse.core.data

import app.twoverse.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** The signed-in user, or null when signed out. */
    val currentUser: Flow<UserProfile?>

    suspend fun signInWithGoogle(): Result<UserProfile>

    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>

    suspend fun signOut()
}
