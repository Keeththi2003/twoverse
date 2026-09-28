package app.twoverse.feature.countdown

import java.time.LocalDate
import java.time.LocalTime

/** Everything the plan editor can ask for, grouped to keep the screen signature readable. */
data class EditReunionActions(
    val onBack: () -> Unit = {},
    val onOpenPicker: (ReunionPicker) -> Unit = {},
    val onDismissPicker: () -> Unit = {},
    val onDateSelected: (LocalDate) -> Unit = {},
    val onHasTimeChange: (Boolean) -> Unit = {},
    val onTimeSelected: (LocalTime) -> Unit = {},
    val onPlaceChange: (String) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onClearRequested: () -> Unit = {},
    val onClearDismissed: () -> Unit = {},
    val onClearConfirmed: () -> Unit = {},
)
