package app.twoverse.core.data.supabase

/** The deep link Supabase Auth redirects to; must match AndroidManifest.xml and the project's redirect URLs. */
internal object AuthCallback {
    const val Scheme = "app.twoverse"
    const val Host = "auth-callback"
    const val PasswordResetPath = "/reset"
    const val PasswordResetUrl = "$Scheme://$Host$PasswordResetPath"
}
