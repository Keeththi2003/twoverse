package app.twoverse.core.data.fake

import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Starts signed out; any sign-in succeeds as the sample user. */
@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {
    private val user = MutableStateFlow<UserProfile?>(null)

    override val currentUser: StateFlow<UserProfile?> = user

    override suspend fun signInWithGoogle(): Result<UserProfile> = signIn()

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> = signIn()

    override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.success(Unit)

    override suspend fun signOut() {
        user.value = null
    }

    private fun signIn(): Result<UserProfile> {
        user.value = SampleData.me
        return Result.success(SampleData.me)
    }
}
