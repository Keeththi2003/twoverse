package app.twoverse.core.model

data class BirthdayWelcome(
    val message: String,
    val fromName: String,
    val photoUrl: String?,
    val seen: Boolean,
)
