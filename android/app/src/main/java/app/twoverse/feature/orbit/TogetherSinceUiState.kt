package app.twoverse.feature.orbit

import app.twoverse.core.model.DataError
import java.time.LocalDate

/** "When did your story begin?" (FR-ORB-1, FR-ORB-2). */
data class TogetherSinceUiState(
    /** Asked right after pairing: it can be skipped instead of going back. */
    val isAfterPairing: Boolean,
    /** The user's local date; later dates can't be picked. */
    val today: LocalDate,
    val isLoading: Boolean = true,
    val date: LocalDate? = null,
    val isPickerOpen: Boolean = false,
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
    val error: DataError? = null,
) {
    val canSave: Boolean get() = date != null && !isSaving && !isLoading
}
