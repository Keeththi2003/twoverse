package app.twoverse.core.model

data class UserProfile(
    val id: String,
    val displayName: String,
    /** IANA time zone saved by their app (e.g. "Asia/Colombo"); null when not known. */
    val timeZone: String? = null,
)
