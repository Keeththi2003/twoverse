package app.twoverse.feature.orbit

import app.twoverse.core.model.DataError
import app.twoverse.core.model.MeetupDraft
import java.time.Instant
import java.time.LocalDate

/** Adding or editing a meetup (FR-ORB-3, FR-ORB-10). */
data class MeetupEditorUiState(
    val isEditing: Boolean,
    /** The user's local date; meetups start today or earlier. */
    val today: LocalDate,
    val isLoading: Boolean = true,
    /** The meetup to edit is gone (the partner deleted it). */
    val loadError: DataError? = null,
    val startDate: LocalDate? = null,
    /** A visit lasting several days has an end date. */
    val severalDays: Boolean = false,
    val endDate: LocalDate? = null,
    val place: String = "",
    val note: String = "",
    /** Set when recording a passed reunion (FR-ORB-10). */
    val fromReunionAt: Instant? = null,
    val openPicker: MeetupPicker? = null,
    val problem: MeetupProblem? = null,
    val error: DataError? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    val canSave: Boolean get() = !isLoading && loadError == null && !isSaving
}

enum class MeetupPicker { Start, End }

enum class MeetupProblem { StartMissing, StartInFuture, EndBeforeStart }

/** Why a meetup can't be saved yet, or null when it can (FR-ORB-3). */
internal fun meetupProblem(start: LocalDate?, end: LocalDate?, today: LocalDate): MeetupProblem? = when {
    start == null -> MeetupProblem.StartMissing
    start.isAfter(today) -> MeetupProblem.StartInFuture
    end != null && end.isBefore(start) -> MeetupProblem.EndBeforeStart
    else -> null
}

/** What is saved: a one-day meetup has no end date; blank place and note are left out. */
internal fun MeetupEditorUiState.toDraft(start: LocalDate) = MeetupDraft(
    startDate = start,
    endDate = endDate?.takeIf { severalDays && it != start },
    place = place.trim().ifEmpty { null },
    note = note.trim().ifEmpty { null },
    fromReunionAt = fromReunionAt,
)
