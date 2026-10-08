package app.twoverse.feature.location

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.text.partnerName
import app.twoverse.R
import app.twoverse.core.designsystem.component.IconTile
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DataError

private val HeroTileSize = 64.dp
private val HeroIconSize = 28.dp

/** Location permission and sharing setup (FR-LOC-1, FR-LOC-2, FR-LOC-4). */
@Composable
fun LocationSetupScreen(
    uiState: LocationSetupUiState,
    actions: LocationSetupActions,
    modifier: Modifier = Modifier,
) {
    val content = uiState.step.content(actions, hasError = uiState.error != null)
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
    ) {
        if (content == null) return@BoxWithConstraints
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontalWide)
                .padding(top = spacing.xxl, bottom = spacing.sm),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                IconTile(icon = content.icon, size = HeroTileSize, iconSize = HeroIconSize)
                Text(
                    text = if (content.titleNamesPartner) stringResource(content.title, partnerName()) else stringResource(content.title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(content.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
                content.note?.let {
                    Text(text = stringResource(it), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                if (uiState.step == LocationSetupStep.BatteryGuide) {
                    TwoverseCard(shape = TwoverseTheme.shapes.cardSmall, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.location_setup_battery_steps),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                            modifier = Modifier.padding(spacing.md),
                        )
                    }
                }
                uiState.error?.let {
                    Text(
                        text = stringResource(it.messageRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            Column(modifier = Modifier.padding(top = spacing.xl)) {
                content.primary?.let { (label, onClick) ->
                    TwoversePrimaryButton(text = stringResource(label), onClick = onClick, modifier = Modifier.fillMaxWidth())
                }
                content.secondary?.let { (label, onClick) ->
                    TwoverseTextButton(text = stringResource(label), onClick = onClick, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private data class StepContent(
    @param:DrawableRes val icon: Int,
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    /** The title has a %1$s for the partner's name ("Share your location with Ammu"). */
    val titleNamesPartner: Boolean = false,
    @param:StringRes val note: Int? = null,
    val primary: Pair<Int, () -> Unit>? = null,
    val secondary: Pair<Int, () -> Unit>? = null,
)

private fun LocationSetupStep.content(actions: LocationSetupActions, hasError: Boolean): StepContent? = when (this) {
    LocationSetupStep.Checking -> null
    LocationSetupStep.Explain -> StepContent(
        icon = R.drawable.ic_pin,
        title = R.string.location_setup_explain_title,
        titleNamesPartner = true,
        body = R.string.location_setup_explain_body,
        note = R.string.location_setup_explain_note,
        primary = R.string.location_setup_continue to actions.onContinue,
        secondary = R.string.location_setup_not_now to actions.onNotNow,
    )
    LocationSetupStep.Denied -> StepContent(
        icon = R.drawable.ic_pin,
        title = R.string.location_setup_denied_title,
        body = R.string.location_setup_denied_body,
        primary = R.string.location_setup_try_again to actions.onContinue,
        secondary = R.string.location_setup_not_now to actions.onNotNow,
    )
    LocationSetupStep.DeniedForever -> StepContent(
        icon = R.drawable.ic_pin,
        title = R.string.location_setup_denied_title,
        body = R.string.location_setup_denied_settings_body,
        primary = R.string.location_setup_open_settings to actions.onOpenSettings,
        secondary = R.string.location_setup_not_now to actions.onNotNow,
    )
    LocationSetupStep.Background -> StepContent(
        icon = R.drawable.ic_clock,
        title = R.string.location_setup_background_title,
        body = R.string.location_setup_background_body,
        primary = R.string.location_setup_allow_all_the_time to actions.onAllowAllTheTime,
        secondary = R.string.location_setup_while_using to actions.onWhileUsingOnly,
    )
    LocationSetupStep.Enabling -> StepContent(
        icon = R.drawable.ic_pin,
        title = R.string.location_setup_explain_title,
        titleNamesPartner = true,
        body = R.string.location_setup_enabling,
        primary = if (hasError) R.string.location_setup_try_again to actions.onRetry else null,
        secondary = if (hasError) R.string.location_setup_not_now to actions.onNotNow else null,
    )
    LocationSetupStep.BatteryGuide -> StepContent(
        icon = R.drawable.ic_info,
        title = R.string.location_setup_battery_title,
        body = R.string.location_setup_battery_body,
        primary = R.string.location_setup_open_battery_settings to actions.onOpenBatterySettings,
        secondary = R.string.location_setup_done to actions.onBatteryGuideDone,
    )
}

@PreviewLightDark
@Composable
private fun LocationSetupExplainPreview() {
    TwoverseTheme {
        LocationSetupScreen(uiState = LocationSetupUiState(step = LocationSetupStep.Explain), actions = LocationSetupActions())
    }
}

@PreviewLightDark
@Composable
private fun LocationSetupBackgroundPreview() {
    TwoverseTheme {
        LocationSetupScreen(uiState = LocationSetupUiState(step = LocationSetupStep.Background), actions = LocationSetupActions())
    }
}

@PreviewLightDark
@Composable
private fun LocationSetupBatteryPreview() {
    TwoverseTheme {
        LocationSetupScreen(
            uiState = LocationSetupUiState(step = LocationSetupStep.BatteryGuide),
            actions = LocationSetupActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun LocationSetupErrorPreview() {
    TwoverseTheme {
        LocationSetupScreen(
            uiState = LocationSetupUiState(step = LocationSetupStep.Enabling, error = DataError.Network),
            actions = LocationSetupActions(),
        )
    }
}
