package app.twoverse.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.OrbitGraphic
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextField
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DataError
import app.twoverse.core.model.Pronouns

private val OrbitSize = 160.dp

/** "About you": the name you like to be called and your pronouns (FR-PRO-2). */
@Composable
fun AboutYouScreen(
    uiState: AboutYouUiState,
    onShortNameChange: (String) -> Unit,
    onPronounsSelected: (Pronouns) -> Unit,
    onContinue: () -> Unit,
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
                OrbitGraphic(
                    size = OrbitSize,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = spacing.xl),
                )
                Text(
                    text = stringResource(R.string.about_you_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.about_you_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uiState.isLoading) return@Column
                TwoverseTextField(
                    value = uiState.shortName,
                    onValueChange = onShortNameChange,
                    label = stringResource(R.string.about_you_short_name),
                    placeholder = stringResource(R.string.about_you_short_name_placeholder),
                )
                Text(
                    text = stringResource(R.string.about_you_pronouns),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                PronounsPicker(selected = uiState.pronouns, onSelect = onPronounsSelected)
                val message = uiState.problem?.let { stringResource(it.textRes()) } ?: uiState.error?.let { stringResource(it.messageRes()) }
                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            TwoversePrimaryButton(
                text = stringResource(R.string.about_you_continue),
                onClick = onContinue,
                enabled = uiState.canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xl),
            )
        }
    }
}

private fun AboutYouProblem.textRes(): Int = when (this) {
    AboutYouProblem.NameMissing -> R.string.about_you_problem_name
    AboutYouProblem.PronounsMissing -> R.string.about_you_problem_pronouns
}

@PreviewLightDark
@Composable
private fun AboutYouSheHerPreview() {
    TwoverseTheme {
        AboutYouScreen(
            uiState = AboutYouUiState(isLoading = false, shortName = "Ammu", pronouns = Pronouns.She),
            onShortNameChange = {}, onPronounsSelected = {}, onContinue = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun AboutYouHeHimPreview() {
    TwoverseTheme {
        AboutYouScreen(
            uiState = AboutYouUiState(isLoading = false, shortName = "Keeththi", pronouns = Pronouns.He),
            onShortNameChange = {}, onPronounsSelected = {}, onContinue = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun AboutYouTheyThemMissingPreview() {
    TwoverseTheme {
        AboutYouScreen(
            uiState = AboutYouUiState(isLoading = false, shortName = "Alex", pronouns = Pronouns.They, error = DataError.Network),
            onShortNameChange = {}, onPronounsSelected = {}, onContinue = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun AboutYouNotChosenPreview() {
    TwoverseTheme {
        AboutYouScreen(
            uiState = AboutYouUiState(isLoading = false, shortName = "Keeththi", problem = AboutYouProblem.PronounsMissing),
            onShortNameChange = {}, onPronounsSelected = {}, onContinue = {},
        )
    }
}
