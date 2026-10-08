package app.twoverse.feature.star

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.common.formatDate
import app.twoverse.R
import app.twoverse.core.designsystem.component.IconTile
import app.twoverse.core.designsystem.component.SegmentedOptions
import app.twoverse.core.designsystem.component.TwoverseBackButton
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoverseChip
import app.twoverse.core.designsystem.component.TwoverseOutlineButton
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.component.TwoverseTextField
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DataError
import app.twoverse.core.model.StarContent
import app.twoverse.core.model.StarDraftProblem
import app.twoverse.core.model.StarField
import app.twoverse.core.model.StarLayout
import app.twoverse.core.model.StarPhotoFit
import coil3.compose.AsyncImage
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val PhotoHeight = 180.dp
private val PickerIconTileSize = 36.dp
private val PickerIconSize = 18.dp
private val PhotoIconSize = 28.dp
private const val MessageCounterFrom = 900
private const val DatePattern = "EEEEdMMMMy"

/** "Send a Shooting Star": layout, template, the layout's fields, when to show, preview (FR-STAR-1 to FR-STAR-8). */
@Composable
fun StarComposerScreen(
    uiState: StarComposerUiState,
    actions: StarComposerActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        ComposerForm(uiState = uiState, actions = actions)
        if (uiState.isPreviewOpen) {
            StarPreview(
                display = uiState.display,
                isDark = uiState.isPreviewDark ?: TwoverseTheme.colors.isDark,
                onDarkChange = actions.onPreviewDarkChange,
                onClose = actions.onClosePreview,
            )
        }
    }
    when (uiState.openPicker) {
        StarPicker.Date -> StarDatePicker(
            initialDate = (uiState.schedule as? StarSchedule.At)?.date,
            today = uiState.today,
            onDismiss = actions.onDismissPicker,
            onDateSelected = actions.onDateSelected,
        )
        StarPicker.Time -> StarTimePicker(
            initialTime = (uiState.schedule as? StarSchedule.At)?.time ?: DefaultStarTime,
            onDismiss = actions.onDismissPicker,
            onTimeSelected = actions.onTimeSelected,
        )
        null -> Unit
    }
}

@Composable
private fun ComposerForm(uiState: StarComposerUiState, actions: StarComposerActions) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl)
                .padding(bottom = spacing.xs),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    TwoverseBackButton(onClick = actions.onBack)
                    Text(
                        text = stringResource(if (uiState.isEditing) R.string.star_composer_edit_title else R.string.star_composer_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                Text(
                    text = stringResource(R.string.star_composer_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                if (uiState.loadError != null) {
                    ErrorText(stringResource(uiState.loadError.messageRes()))
                    TwoverseTextButton(text = stringResource(R.string.common_retry), onClick = actions.onRetryLoad)
                    return@Column
                }
                if (uiState.isLoading) return@Column
                SectionLabel(R.string.star_composer_layout)
                StarLayoutPicker(selected = uiState.layout, onSelect = actions.onLayoutSelected)
                SectionLabel(R.string.star_composer_template)
                TemplatePicker(selected = uiState.template, onSelect = actions.onTemplateSelected)
                ComposerFields(uiState = uiState, actions = actions)
                SectionLabel(R.string.star_composer_when)
                ScheduleEditor(schedule = uiState.schedule, actions = actions)
                val problem = uiState.problem?.let { stringResource(it.textRes()) }
                val error = uiState.error?.let { stringResource(it.messageRes()) }
                (problem ?: error)?.let { ErrorText(it) }
            }
            if (uiState.loadError == null && !uiState.isLoading) {
                Row(
                    modifier = Modifier.padding(top = spacing.xl),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    TwoverseOutlineButton(
                        text = stringResource(R.string.star_composer_preview),
                        onClick = actions.onOpenPreview,
                        leadingIcon = R.drawable.ic_eye,
                        modifier = Modifier.weight(1f),
                    )
                    TwoversePrimaryButton(
                        text = stringResource(if (uiState.isEditing) R.string.star_composer_save else R.string.star_composer_send),
                        onClick = actions.onSend,
                        enabled = uiState.canSend,
                        leadingIcon = R.drawable.ic_send,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Only the fields the layout uses (FR-STAR-4). */
@Composable
private fun ComposerFields(uiState: StarComposerUiState, actions: StarComposerActions) {
    if (uiState.shows(StarField.Photo)) {
        PhotoPicker(
            photo = uiState.photo,
            required = uiState.layout == StarLayout.FullPhoto,
            onPickPhoto = actions.onPickPhoto,
            onRemovePhoto = actions.onRemovePhoto,
        )
    }
    if (uiState.shows(StarField.PhotoFit)) {
        SegmentedOptions(
            options = listOf(stringResource(R.string.star_fit_fill), stringResource(R.string.star_fit_fit)),
            selectedIndex = if (uiState.photoFit == StarPhotoFit.Fit) 1 else 0,
            onSelect = { actions.onPhotoFitSelected(if (it == 1) StarPhotoFit.Fit else StarPhotoFit.Fill) },
        )
    }
    if (uiState.shows(StarField.Eyebrow)) {
        TwoverseTextField(
            value = uiState.eyebrow,
            onValueChange = actions.onEyebrowChange,
            label = stringResource(R.string.star_field_eyebrow),
            placeholder = stringResource(R.string.star_field_eyebrow_placeholder),
        )
    }
    if (uiState.shows(StarField.Title)) {
        TwoverseTextField(
            value = uiState.title,
            onValueChange = actions.onTitleChange,
            label = stringResource(R.string.star_field_title),
            placeholder = stringResource(R.string.star_field_title_placeholder),
        )
    }
    if (uiState.shows(StarField.Message)) {
        MessageEditor(message = uiState.message, onMessageChange = actions.onMessageChange)
    }
    if (uiState.shows(StarField.Signature)) {
        TwoverseTextField(
            value = uiState.signature,
            onValueChange = actions.onSignatureChange,
            label = stringResource(R.string.star_field_signature),
            placeholder = stringResource(R.string.star_field_signature_placeholder),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplatePicker(selected: StarTemplate?, onSelect: (StarTemplate, StarTemplateText) -> Unit) {
    val texts = StarTemplate.entries.associateWith { template ->
        StarTemplateText(
            eyebrow = template.eyebrowRes?.let { stringResource(it) }.orEmpty(),
            title = template.titleRes?.let { stringResource(it) }.orEmpty(),
        )
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
    ) {
        StarTemplate.entries.forEach { template ->
            TwoverseChip(
                label = stringResource(template.labelRes),
                selected = template == selected,
                onClick = { onSelect(template, texts.getValue(template)) },
            )
        }
    }
}

@Composable
private fun MessageEditor(message: String, onMessageChange: (String) -> Unit) {
    Column {
        TwoverseTextField(
            value = message,
            onValueChange = onMessageChange,
            label = stringResource(R.string.star_field_message),
            placeholder = stringResource(R.string.star_field_message_placeholder),
            singleLine = false,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
        )
        if (message.length >= MessageCounterFrom) {
            Text(
                text = stringResource(R.string.star_field_count, message.length, StarContent.MaxMessageLength),
                style = MaterialTheme.typography.bodySmall,
                color = TwoverseTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwoverseTheme.spacing.xxs),
            )
        }
    }
}

@Composable
private fun PhotoPicker(photo: String?, required: Boolean, onPickPhoto: () -> Unit, onRemovePhoto: () -> Unit) {
    val colors = TwoverseTheme.colors
    val addLabel = stringResource(if (required) R.string.star_photo_add_required else R.string.star_photo_add)
    val description = if (photo == null) addLabel else stringResource(R.string.star_photo_change)
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PhotoHeight)
                .clip(TwoverseTheme.shapes.card)
                .background(colors.surface)
                .clickable(role = Role.Button, onClick = onPickPhoto)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(PhotoIconSize),
                    )
                    Text(text = addLabel, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                }
            }
        }
        if (photo != null) {
            TwoverseTextButton(
                text = stringResource(R.string.star_photo_remove),
                onClick = onRemovePhoto,
                color = colors.error,
            )
        }
    }
}

/** Next open, or a chosen date and time (FR-STAR-7). */
@Composable
private fun ScheduleEditor(schedule: StarSchedule, actions: StarComposerActions) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm)) {
        SegmentedOptions(
            options = listOf(stringResource(R.string.star_when_next_open), stringResource(R.string.star_when_date_time)),
            selectedIndex = if (schedule is StarSchedule.At) 1 else 0,
            onSelect = { if (it == 0) actions.onShowNextOpen() else actions.onOpenPicker(StarPicker.Date) },
        )
        when (schedule) {
            StarSchedule.NextOpen -> Text(
                text = stringResource(R.string.star_when_next_open_description),
                style = MaterialTheme.typography.bodySmall,
                color = TwoverseTheme.colors.onSurfaceVariant,
            )
            is StarSchedule.At -> {
                PickerRow(
                    icon = R.drawable.ic_calendar,
                    label = stringResource(R.string.star_when_date),
                    value = formatDate(schedule.date, DatePattern, locale),
                    onClick = { actions.onOpenPicker(StarPicker.Date) },
                )
                PickerRow(
                    icon = R.drawable.ic_clock,
                    label = stringResource(R.string.star_when_time),
                    value = schedule.time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)),
                    onClick = { actions.onOpenPicker(StarPicker.Time) },
                )
            }
        }
    }
}

@Composable
private fun PickerRow(@DrawableRes icon: Int, label: String, value: String, onClick: () -> Unit) {
    val colors = TwoverseTheme.colors
    TwoverseCard(shape = TwoverseTheme.shapes.cardSmall, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TwoverseTheme.spacing.minTouchTarget)
                .padding(TwoverseTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.smd),
        ) {
            IconTile(icon = icon, size = PickerIconTileSize, iconSize = PickerIconSize)
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = colors.onSurfaceVariant,
                )
                Text(text = value, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            }
        }
    }
}

@Composable
private fun SectionLabel(@StringRes text: Int) {
    Text(
        text = stringResource(text).uppercase(LocalConfiguration.current.locales[0]),
        style = TwoverseTheme.textStyles.sectionHeader,
        color = TwoverseTheme.colors.onSurfaceVariant,
        modifier = Modifier
            .padding(top = TwoverseTheme.spacing.xs)
            .semantics { heading() },
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TwoverseTheme.colors.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

private fun StarDraftProblem.textRes(): Int = when (this) {
    StarDraftProblem.PhotoRequired -> R.string.star_problem_photo_required
    StarDraftProblem.NothingToShow -> R.string.star_problem_nothing_to_show
    StarDraftProblem.ShowAtInPast -> R.string.star_problem_in_past
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 9, 29)

@PreviewLightDark
@Composable
private fun StarComposerScreenPreview() {
    TwoverseTheme {
        StarComposerScreen(
            uiState = StarComposerUiState(
                isEditing = false,
                today = PreviewToday,
                isLoading = false,
                template = StarTemplate.Birthday,
                eyebrow = "For you",
                title = "Happy Birthday",
                message = "I built a little universe, just for the two of us.",
                signature = "Keeththi",
                schedule = StarSchedule.At(PreviewToday.plusDays(13), LocalTime.of(9, 0)),
            ),
            actions = StarComposerActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun StarComposerFullPhotoPreview() {
    TwoverseTheme {
        StarComposerScreen(
            uiState = StarComposerUiState(
                isEditing = true,
                today = PreviewToday,
                isLoading = false,
                layout = StarLayout.FullPhoto,
                photoFit = StarPhotoFit.Fit,
                problem = StarDraftProblem.PhotoRequired,
            ),
            actions = StarComposerActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun StarComposerLoadErrorPreview() {
    TwoverseTheme {
        StarComposerScreen(
            uiState = StarComposerUiState(isEditing = true, today = PreviewToday, isLoading = false, loadError = DataError.StarUnavailable),
            actions = StarComposerActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun StarComposerPreviewOpenPreview() {
    TwoverseTheme {
        StarComposerScreen(
            uiState = StarComposerUiState(
                isEditing = false,
                today = PreviewToday,
                isLoading = false,
                layout = StarLayout.MessageOnly,
                title = "Thinking of you",
                message = "Wherever you are right now, I hope you feel this.",
                isPreviewOpen = true,
            ),
            actions = StarComposerActions(),
        )
    }
}
