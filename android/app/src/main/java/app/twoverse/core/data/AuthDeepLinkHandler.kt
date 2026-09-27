package app.twoverse.core.data

import android.content.Intent

/** Finishes auth flows that return to the app through app.twoverse://auth-callback. */
interface AuthDeepLinkHandler {
    /** Handles [intent] if it is an auth callback; [onPasswordRecovery] runs once a reset link is accepted. */
    fun handle(intent: Intent, onPasswordRecovery: () -> Unit)
}
