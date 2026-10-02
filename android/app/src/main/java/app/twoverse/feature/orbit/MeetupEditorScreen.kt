package app.twoverse.feature.orbit

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.IconTile
import app.twoverse.core.designsystem.component.SettingsSwitchRow
import app.twoverse.core.designsystem.component.TwoverseBackButton
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextField
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DataError
import app.twoverse.core.model.MeetupDraft
import java.time.LocalDate

private val PickerIconTileSize = 36.dp
private val PickerIconSize = 18.dp
private const val NoteCounterFrom = 250

/** Everything the meetup editor can ask for. */
data class MeetupEditorActions(
    val onBack: () -> Unit = {},
    val onOpenPicker: (MeetupPicker) -> Unit = {},
    val onDismissPicker: () -> Unit = {},
    val onStartDateSelected: (LocalDate) -> Unit = {},
    val onEndDateSelected: (LocalDate) -> Unit = {},
    val onSeveralDaysChange: (Boolean) -> Unit = {},
    val onPlaceChange: (String) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
)

/** Add or edit a meetup: dates, place and note (FR-ORB-3, FR-ORB-10). */
@Composable
fun MeetupEditorScreen(uiState: MeetupEditorUiState, actions: MeetupEditorActions, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    BoxWithConstraints(
        modifier = modifier
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    TwoverseBackButton(onClick = actions.onBack)
                    Text(
                        text = stringResource(if (uiState.isEditing) R.string.orbit_meetup_edit_title else R.string.orbit_meetup_add_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                if (uiState.loadError != null) {
                    ErrorText(stringResource(uiState.loadError.messageRes()))
                    return@Column
                }
                if (uiState.isLoading) return@Column
                PickerRow(
                    icon = R.drawable.ic_calendar,
                    label = stringResource(if (uiState.severalDays) R.string.orbit_meetup_first_day else R.string.orbit_meetup_date),
                    value = uiState.startDate?.let { longDate(it) } ?: stringResource(R.string.orbit_meetup_choose_date),
                    onClick = { actions.onOpenPicker(MeetupPicker.Start) },
                )
                TwoverseCard(shape = TwoverseTheme.shapes.cardSmall, modifier = Modifier.fillMaxWidth()) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.orbit_meetup_several_days),
                        checked = uiState.severalDays,
                        onCheckedChange = actions.onSeveralDaysChange,
                    )
                }
                if (uiState.severalDays) {
                    PickerRow(
                        icon = R.drawable.ic_calendar,
                        label = stringResource(R.string.orbit_meetup_last_day),
                        value = uiState.endDate?.let { longDate(it) } ?: stringResource(R.string.orbit_meetup_choose_date),
                        onClick = { actions.onOpenPicker(MeetupPicker.End) },
                    )
                }
                TwoverseTextField(
                    value = uiState.place,
                    onValueChange = actions.onPlaceChange,
                    label = stringResource(R.string.orbit_meetup_place),
                    placeholder = stringResource(R.string.orbit_meetup_place_placeholder),
                )
                Column {
                    TwoverseTextField(
                        value = uiState.note,
                        onValueChange = actions.onNoteChange,
                        label = stringResource(R.string.orbit_meetup_note),
                        placeholder = stringResource(R.string.orbit_meetup_note_placeholder),
                        singleLine = false,
                    )
                    if (uiState.note.length >= NoteCounterFrom) {
                        Text(
                            text = stringResource(R.string.orbit_meetup_note_count, uiState.note.length, MeetupDraft.MaxNoteLength),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = spacing.xxs),
                        )
                    }
                }
                val problem = uiState.problem?.let { stringResource(it.textRes()) }
                val error = uiState.error?.let { stringResource(it.messageRes()) }
                (problem ?: error)?.let { ErrorText(it) }
            }
            TwoversePrimaryButton(
                text = stringResource(R.string.orbit_meetup_save),
                onClick = actions.onSave,
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xl),
            )
        }
    }
    when (uiState.openPicker) {
        MeetupPicker.Start -> OrbitDatePicker(
            initialDate = uiState.startDate ?: uiState.today,
            maxDate = uiState.today,
            onDismiss = actions.onDismissPicker,
            onDateSelected = actions.onStartDateSelected,
        )
        MeetupPicker.End -> OrbitDatePicker(
            initialDate = uiState.endDate ?: uiState.startDate ?: uiState.today,
            minDate = uiState.startDate,
            onDismiss = actions.onDismissPicker,
            onDateSelected = actions.onEndDateSelected,
        )
        null -> Unit
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
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TwoverseTheme.colors.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

private fun MeetupProblem.textRes(): Int = when (this) {
    MeetupProblem.StartMissing -> R.string.orbit_meetup_problem_start_missing
    MeetupProblem.StartInFuture -> R.string.error_date_in_future
    MeetupProblem.EndBeforeStart -> R.string.orbit_meetup_problem_end_before_start
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 12)

@PreviewLightDark
@Composable
private fun MeetupEditorFromReunionPreview() {
    TwoverseTheme {
        MeetupEditorScreen(
            uiState = MeetupEditorUiState(
                isEditing = false,
                today = PreviewToday,
                isLoading = false,
                startDate = LocalDate.of(2026, 10, 10),
                severalDays = true,
                endDate = LocalDate.of(2026, 10, 12),
                place = "Kandy",
            ),
            actions = MeetupEditorActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun MeetupEditorEmptyPreview() {
    TwoverseTheme {
        MeetupEditorScreen(
            uiState = MeetupEditorUiState(
                isEditing = true,
                today = PreviewToday,
                isLoading = false,
                problem = MeetupProblem.StartMissing,
                error = DataError.Network,
            ),
            actions = MeetupEditorActions(),
        )
    }
}
