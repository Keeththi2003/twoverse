package app.twoverse.core.data.supabase

import app.twoverse.core.data.AuthRepository
import app.twoverse.core.model.AuthState
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.SignUpResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : AuthRepository {

    override val authState: Flow<AuthState> = supabase.auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Initializing -> AuthState.Loading
                is SessionStatus.Authenticated -> status.session.user?.id?.let(AuthState::SignedIn) ?: AuthState.SignedOut
                // Offline with a saved session: stay signed in and show cached data (NFR-REL-1).
                is SessionStatus.RefreshFailure -> supabase.auth.currentUserOrNull()?.id?.let(AuthState::SignedIn)
                    ?: AuthState.SignedOut
                is SessionStatus.NotAuthenticated -> AuthState.SignedOut
            }
        }
        .distinctUntilChanged()

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): DataResult<Unit> = supabaseCall {
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
            nonce = rawNonce
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): DataResult<Unit> = supabaseCall {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUpWithEmail(fullName: String, email: String, password: String): DataResult<SignUpResult> =
        supabaseCall {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                // The profile trigger also sets the short name from it (FR-PRO-1).
                data = buildJsonObject { put("full_name", fullName) }
            }
            if (supabase.auth.currentSessionOrNull() != null) SignUpResult.SignedIn else SignUpResult.ConfirmEmail
        }

    override suspend fun sendPasswordReset(email: String): DataResult<Unit> = supabaseCall {
        supabase.auth.resetPasswordForEmail(email, redirectUrl = AuthCallback.PasswordResetUrl)
    }

    override suspend fun updatePassword(newPassword: String): DataResult<Unit> = supabaseCall {
        supabase.auth.updateUser { password = newPassword }
    }

    override suspend fun signOut(): DataResult<Unit> = supabaseCall {
        supabase.auth.signOut()
    }

    /** The server deletes the user (and so their sessions); the local session is then cleared. */
    override suspend fun deleteAccount(): DataResult<Unit> = supabaseCall {
        supabase.postgrest.rpc("delete_account")
        supabase.auth.clearSession()
    }
}
