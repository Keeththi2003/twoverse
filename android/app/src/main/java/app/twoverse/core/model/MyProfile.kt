package app.twoverse.core.model

/** The signed-in user's own profile (FR-PRO-4). */
data class MyProfile(
    val fullName: String,
    val shortName: String,
    /** Null until chosen; the app then asks on the About you step (FR-PRO-2). */
    val pronouns: Pronouns?,
    /** International format, e.g. +94771234567. */
    val phone: String?,
    val shareEmail: Boolean,
    val sharePhone: Boolean,
    val email: String?,
    /** A new email waiting for confirmation; the old one stays until then. */
    val pendingEmail: String?,
    /** False for Google-only accounts, whose email comes from Google. */
    val canChangeEmail: Boolean,
)

/** What the user saves on their profile; null fields stay as they are. */
data class ProfileEdit(
    val fullName: String? = null,
    val shortName: String? = null,
    val pronouns: Pronouns? = null,
    /** Set [clearPhone] to remove the number. */
    val phone: String? = null,
    val clearPhone: Boolean = false,
    val shareEmail: Boolean? = null,
    val sharePhone: Boolean? = null,
) {
    companion object {
        /** Match the profiles table. */
        const val MaxFullNameLength = 50
    }
}
