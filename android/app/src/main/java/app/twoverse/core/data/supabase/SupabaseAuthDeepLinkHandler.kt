package app.twoverse.core.data.supabase

import android.content.Intent
import app.twoverse.core.data.AuthDeepLinkHandler
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthDeepLinkHandler @Inject constructor(
    private val supabase: SupabaseClient,
) : AuthDeepLinkHandler {

    override fun handle(intent: Intent, onPasswordRecovery: () -> Unit) {
        val uri = intent.data ?: return
        if (uri.scheme != AuthCallback.Scheme || uri.host != AuthCallback.Host) return
        val isPasswordReset = uri.path == AuthCallback.PasswordResetPath
        supabase.handleDeeplinks(intent, onSessionSuccess = { if (isPasswordReset) onPasswordRecovery() })
    }
}
