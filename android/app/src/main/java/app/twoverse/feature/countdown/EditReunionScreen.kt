package app.twoverse.feature.countdown

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.common.formatDate
import app.twoverse.R
import app.twoverse.core.designsystem.component.IconTile
import app.twoverse.core.designsystem.component.SettingsSwitchRow
import app.twoverse.core.designsystem.component.TwoverseBackButton
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoverseConfirmDialog
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.component.TwoverseTextField
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val PickerIconTileSize = 36.dp
private val PickerIconSize = 18.dp
private const val DatePattern = "EEEEdMMMMy"

/** Set, edit or clear the shared reunion (FR-CNT-1). */
@Composable
fun EditReunionScreen(
    uiState: EditReunionUiState,
    actions: EditReunionActions,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    val locale = LocalConfiguration.current.locales[0]
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
    ) {
        if (uiState.isLoading) return@BoxWithConstraints
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
                        text = stringResource(R.string.edit_reunion_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                PickerRow(
                    icon = R.drawable.ic_calendar,
                    label = stringResource(R.string.edit_reunion_date),
                    value = uiState.date?.let { formatDate(it, DatePattern, locale) }
                        ?: stringResource(R.string.edit_reunion_choose_date),
                    onClick = { actions.onOpenPicker(ReunionPicker.Date) },
                )
                TwoverseCard(shape = TwoverseTheme.shapes.cardSmall, modifier = Modifier.fillMaxWidth()) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.edit_reunion_add_time),
                        checked = uiState.hasTime,
                        onCheckedChange = actions.onHasTimeChange,
                    )
                }
                if (uiState.hasTime) {
                    PickerRow(
                        icon = R.drawable.ic_clock,
                        label = stringResource(R.string.edit_reunion_time),
                        value = uiState.time?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
                            ?: stringResource(R.string.edit_reunion_choose_time),
                        onClick = { actions.onOpenPicker(ReunionPicker.Time) },
                    )
                }
                TwoverseTextField(
                    value = uiState.place,
                    onValueChange = actions.onPlaceChange,
                    label = stringResource(R.string.edit_reunion_place_label),
                    placeholder = stringResource(R.string.edit_reunion_place_placeholder),
                )
                TwoverseTextField(
                    value = uiState.note,
                    onValueChange = actions.onNoteChange,
                    label = stringResource(R.string.edit_reunion_note_label),
                    placeholder = stringResource(R.string.edit_reunion_note_placeholder),
                    singleLine = false,
                )
                val message = uiState.problem?.textRes() ?: uiState.error?.messageRes()
                message?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            Column(modifier = Modifier.padding(top = spacing.xl)) {
                TwoversePrimaryButton(
                    text = stringResource(R.string.edit_reunion_save),
                    onClick = actions.onSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uiState.canClear) {
                    TwoverseTextButton(
                        text = stringResource(R.string.edit_reunion_clear),
                        onClick = actions.onClearRequested,
                        color = colors.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    when (uiState.openPicker) {
        ReunionPicker.Date -> ReunionDatePicker(
            initialDate = uiState.date,
            today = uiState.today,
            onDismiss = actions.onDismissPicker,
            onDateSelected = actions.onDateSelected,
        )
        ReunionPicker.Time -> ReunionTimePicker(
            initialTime = uiState.time,
            onDismiss = actions.onDismissPicker,
            onTimeSelected = actions.onTimeSelected,
        )
        null -> Unit
    }
    if (uiState.isClearConfirmOpen) {
        TwoverseConfirmDialog(
            title = stringResource(R.string.edit_reunion_clear_title),
            message = stringResource(R.string.edit_reunion_clear_message),
            confirmLabel = stringResource(R.string.edit_reunion_clear),
            onConfirm = actions.onClearConfirmed,
            onDismiss = actions.onClearDismissed,
            destructive = true,
        )
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

private fun EditReunionProblem.textRes(): Int = when (this) {
    EditReunionProblem.DateMissing -> R.string.edit_reunion_date_missing
    EditReunionProblem.InPast -> R.string.edit_reunion_in_past
}

@PreviewLightDark
@Composable
private fun EditReunionScreenPreview() {
    TwoverseTheme {
        EditReunionScreen(
            uiState = EditReunionUiState(
                isLoading = false,
                date = LocalDate.of(2026, 10, 10),
                hasTime = true,
                time = LocalTime.of(10, 0),
                place = "Kandy",
                note = "Bring the camera",
                canClear = true,
            ),
            actions = EditReunionActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun EditReunionScreenEmptyPreview() {
    TwoverseTheme {
        EditReunionScreen(
            uiState = EditReunionUiState(isLoading = false, problem = EditReunionProblem.DateMissing),
            actions = EditReunionActions(),
        )
    }
}
