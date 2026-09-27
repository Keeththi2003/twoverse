package app.twoverse.core.data

/** This device's push token, or null when push isn't available. */
fun interface PushTokenSource {
    suspend fun token(): String?
}
