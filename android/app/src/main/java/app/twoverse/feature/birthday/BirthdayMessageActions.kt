package app.twoverse.feature.birthday

import java.time.LocalDate

/** Everything the birthday message editor can ask for. */
data class BirthdayMessageActions(
    val onBack: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onMessageChange: (String) -> Unit = {},
    val onPickPhoto: () -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onOpenDatePicker: () -> Unit = {},
    val onDismissDatePicker: () -> Unit = {},
    val onDateSelected: (LocalDate) -> Unit = {},
    val onClearDate: () -> Unit = {},
    val onSave: () -> Unit = {},
)
