package app.twoverse.core.data.fake

import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.AuthState
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.SignUpResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * In-memory auth for previews and tests. Starts signed out; every call succeeds as the
 * sample user unless a test sets [failNextWith].
 */
class FakeAuthRepository @Inject constructor() : AuthRepository {
    private val state = MutableStateFlow<AuthState>(AuthState.SignedOut)

    override val authState: StateFlow<AuthState> = state

    /** The next call fails with this error (then it resets). */
    var failNextWith: DataError? = null

    /** What email sign-up returns when it succeeds. */
    var signUpResult: SignUpResult = SignUpResult.SignedIn

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): DataResult<Unit> = signIn()

    override suspend fun signInWithEmail(email: String, password: String): DataResult<Unit> = signIn()

    override suspend fun signUpWithEmail(displayName: String, email: String, password: String): DataResult<SignUpResult> =
        respond {
            if (signUpResult == SignUpResult.SignedIn) state.value = AuthState.SignedIn(SampleData.me.id)
            signUpResult
        }

    override suspend fun sendPasswordReset(email: String): DataResult<Unit> = respond { }

    override suspend fun updatePassword(newPassword: String): DataResult<Unit> = respond { }

    override suspend fun signOut(): DataResult<Unit> = respond { state.value = AuthState.SignedOut }

    override suspend fun deleteAccount(): DataResult<Unit> = respond { state.value = AuthState.SignedOut }

    private fun signIn(): DataResult<Unit> = respond { state.value = AuthState.SignedIn(SampleData.me.id) }

    private fun <T> respond(action: () -> T): DataResult<T> {
        val error = failNextWith
        failNextWith = null
        return if (error != null) DataResult.Failure(error) else DataResult.Success(action())
    }
}
