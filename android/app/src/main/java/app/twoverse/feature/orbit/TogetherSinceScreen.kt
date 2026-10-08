package app.twoverse.feature.orbit

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
import app.twoverse.core.designsystem.component.OrbitGraphic
import app.twoverse.core.designsystem.component.TwoverseBackButton
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DataError
import java.time.LocalDate

private val OrbitSize = 180.dp
private val PickerIconTileSize = 36.dp
private val PickerIconSize = 18.dp

/** "When did your story begin?" with a date picker; skippable right after pairing (FR-ORB-2). */
@Composable
fun TogetherSinceScreen(
    uiState: TogetherSinceUiState,
    onBack: () -> Unit,
    onOpenPicker: () -> Unit,
    onDismissPicker: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                if (!uiState.isAfterPairing) TwoverseBackButton(onClick = onBack)
                OrbitGraphic(
                    size = OrbitSize,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = spacing.xl),
                )
                Text(
                    text = stringResource(R.string.orbit_story_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.orbit_story_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!uiState.isLoading) DateRow(date = uiState.date, onClick = onOpenPicker)
                uiState.error?.let { error ->
                    Text(
                        text = stringResource(error.messageRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            Column(modifier = Modifier.padding(top = spacing.xl)) {
                TwoversePrimaryButton(
                    text = stringResource(R.string.orbit_story_save),
                    onClick = onSave,
                    enabled = uiState.canSave,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uiState.isAfterPairing) {
                    TwoverseTextButton(
                        text = stringResource(R.string.orbit_story_skip),
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    if (uiState.isPickerOpen) {
        OrbitDatePicker(
            initialDate = uiState.date ?: uiState.today,
            maxDate = uiState.today,
            onDismiss = onDismissPicker,
            onDateSelected = onDateSelected,
        )
    }
}

@Composable
private fun DateRow(date: LocalDate?, onClick: () -> Unit) {
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
            IconTile(icon = R.drawable.ic_heart, size = PickerIconTileSize, iconSize = PickerIconSize)
            Column {
                Text(
                    text = stringResource(R.string.orbit_story_date_label),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = date?.let { longDate(it) } ?: stringResource(R.string.orbit_story_choose_date),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
            }
        }
    }
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 2)

@PreviewLightDark
@Composable
private fun TogetherSinceAfterPairingPreview() {
    TwoverseTheme {
        TogetherSinceScreen(
            uiState = TogetherSinceUiState(isAfterPairing = true, today = PreviewToday, isLoading = false),
            onBack = {}, onOpenPicker = {}, onDismissPicker = {}, onDateSelected = {}, onSave = {}, onSkip = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun TogetherSinceChangePreview() {
    TwoverseTheme {
        TogetherSinceScreen(
            uiState = TogetherSinceUiState(
                isAfterPairing = false,
                today = PreviewToday,
                isLoading = false,
                date = LocalDate.of(2024, 2, 14),
                error = DataError.DateInFuture,
            ),
            onBack = {}, onOpenPicker = {}, onDismissPicker = {}, onDateSelected = {}, onSave = {}, onSkip = {},
        )
    }
}
