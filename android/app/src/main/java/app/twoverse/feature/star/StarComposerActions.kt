package app.twoverse.feature.star

import app.twoverse.core.model.StarLayout
import app.twoverse.core.model.StarPhotoFit
import java.time.LocalDate
import java.time.LocalTime

/** Everything the Shooting Star composer can ask for. */
data class StarComposerActions(
    val onBack: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onLayoutSelected: (StarLayout) -> Unit = {},
    val onTemplateSelected: (StarTemplate, StarTemplateText) -> Unit = { _, _ -> },
    val onEyebrowChange: (String) -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onMessageChange: (String) -> Unit = {},
    val onSignatureChange: (String) -> Unit = {},
    val onPickPhoto: () -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onPhotoFitSelected: (StarPhotoFit) -> Unit = {},
    val onShowNextOpen: () -> Unit = {},
    val onOpenPicker: (StarPicker) -> Unit = {},
    val onDismissPicker: () -> Unit = {},
    val onDateSelected: (LocalDate) -> Unit = {},
    val onTimeSelected: (LocalTime) -> Unit = {},
    val onOpenPreview: () -> Unit = {},
    val onPreviewDarkChange: (Boolean) -> Unit = {},
    val onClosePreview: () -> Unit = {},
    val onSend: () -> Unit = {},
)
