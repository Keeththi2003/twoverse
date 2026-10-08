package app.twoverse.core.model

/** A user as the signed-in user sees them: themselves, or their partner (FR-PRO-1). */
data class UserProfile(
    val id: String,
    /** Full name from Google or the sign-up form; shown only on profile screens. */
    val fullName: String,
    /** What they like to be called; null only before it is known. */
    val shortName: String? = null,
    val pronouns: Pronouns? = null,
    /** IANA time zone saved by their app (e.g. "Asia/Colombo"); null when not known. */
    val timeZone: String? = null,
    /** For the partner: the signed-in user's private nickname for them (only they see it). */
    val nickname: String? = null,
    /** For the partner: their email, only when they share it (FR-PRO-5). */
    val email: String? = null,
    /** For the partner: their phone number, only when they share it (FR-PRO-5). */
    val phone: String? = null,
)
