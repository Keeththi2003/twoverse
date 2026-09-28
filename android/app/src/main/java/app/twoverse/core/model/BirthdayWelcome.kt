package app.twoverse.core.model

import java.time.LocalDate

/** A birthday welcome the partner wrote for this user (FR-BDY-1 to FR-BDY-4). */
data class BirthdayWelcome(
    val message: String,
    val fromName: String,
    /** Short-lived signed URL of the optional photo (NFR-SEC-2). */
    val photoUrl: String?,
    /** Shown from this local date; null shows it the next time the app is opened. */
    val showOn: LocalDate?,
    val seen: Boolean,
)

/** Shown once, when unseen and its date (if any) has come (FR-BDY-3). */
fun BirthdayWelcome.isDue(today: LocalDate): Boolean = !seen && (showOn == null || !today.isBefore(showOn))

/** The welcome this user wrote for their partner (FR-BDY-1). */
data class BirthdayMessage(
    val message: String,
    val showOn: LocalDate?,
    /** Short-lived signed URL of the saved photo, if there is one. */
    val photoUrl: String?,
    /** The partner has already opened it. */
    val seen: Boolean,
)

/** What the author saves (FR-BDY-1). */
data class BirthdayMessageDraft(
    val message: String,
    val showOn: LocalDate?,
    val photo: BirthdayPhotoChange,
) {
    companion object {
        /** Matches the birthday_welcomes table. */
        const val MaxMessageLength = 1000
    }
}

sealed interface BirthdayPhotoChange {
    data object Keep : BirthdayPhotoChange

    data object Remove : BirthdayPhotoChange

    data class Replace(val photoUri: String) : BirthdayPhotoChange
}
