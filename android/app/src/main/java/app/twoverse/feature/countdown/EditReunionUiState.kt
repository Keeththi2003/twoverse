package app.twoverse.feature.countdown

import app.twoverse.core.model.DataError
import java.time.LocalDate
import java.time.LocalTime

data class EditReunionUiState(
    val isLoading: Boolean = true,
    val date: LocalDate? = null,
    /** Optional time (FR-CNT-1); without one the whole day counts. */
    val hasTime: Boolean = false,
    val time: LocalTime? = null,
    val place: String = "",
    val note: String = "",
    /** A date is already set, so it can be cleared. */
    val canClear: Boolean = false,
    /** The user's local date; the date picker starts here. */
    val today: LocalDate = LocalDate.MIN,
    val openPicker: ReunionPicker? = null,
    val isClearConfirmOpen: Boolean = false,
    val problem: EditReunionProblem? = null,
    val error: DataError? = null,
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
)

enum class ReunionPicker { Date, Time }

enum class EditReunionProblem { DateMissing, InPast }
