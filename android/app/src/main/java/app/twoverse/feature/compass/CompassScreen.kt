package app.twoverse.feature.compass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.common.LocationUnavailableReason
import app.twoverse.core.designsystem.component.TwoverseStatusChip
import app.twoverse.core.designsystem.text.labelRes
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.text.shortLabelRes
import app.twoverse.core.designsystem.text.shortText
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DistanceUnit

private val DialMaxSize = 300.dp
private val TipIconSize = 20.dp

/** Your Star (FR-CMP-1 to FR-CMP-6). The needle follows `needleRotation` from the UiState. */
@Composable
fun CompassScreen(
    uiState: CompassUiState,
    needleRotation: () -> Float?,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        if (uiState !is CompassUiState.Success) return@Box
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(horizontal = spacing.xl)
                .padding(bottom = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.compass_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.compass_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.xs),
                )
            }
            Spacer(modifier = Modifier.height(spacing.xl))
            if (uiState.hasCompassSensor) {
                CompassDial(
                    needleRotation = { if (uiState.showsNeedle) needleRotation() else null },
                    contentDescription = dialDescription(uiState.direction),
                    modifier = Modifier
                        .widthIn(max = DialMaxSize)
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            } else {
                NoCompassDirection(direction = uiState.direction)
            }
            Spacer(modifier = Modifier.height(spacing.lg))
            DistanceAndDirection(uiState = uiState)
            Spacer(modifier = Modifier.height(spacing.md))
            StatusChips(uiState = uiState)
            Spacer(modifier = Modifier.height(spacing.lg))
            TipCard(
                textRes = when {
                    !uiState.hasCompassSensor -> R.string.compass_no_sensor
                    uiState.isCalibrated -> R.string.compass_tip
                    else -> R.string.compass_calibrate_hint
                },
            )
        }
    }
}

@Composable
private fun dialDescription(direction: CompassDirection?): String =
    if (direction != null) {
        stringResource(
            R.string.compass_dial_description,
            stringResource(direction.labelRes()).lowercase(LocalConfiguration.current.locales[0]),
        )
    } else {
        stringResource(R.string.compass_dial_no_direction)
    }

@Composable
private fun DistanceAndDirection(uiState: CompassUiState.Success) {
    val colors = TwoverseTheme.colors
    val distance = uiState.distance
    val direction = uiState.direction
    val bearing = uiState.bearingDegrees
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xxs),
    ) {
        if (distance == null || direction == null || bearing == null) {
            Text(
                text = stringResource(uiState.unavailableReason?.messageRes() ?: R.string.location_partner_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            return@Column
        }
        Text(
            text = stringResource(R.string.compass_distance, distance, stringResource(uiState.distanceUnit.shortLabelRes())),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onSurface,
        )
        Text(
            text = stringResource(R.string.compass_direction_bearing, stringResource(direction.labelRes()), bearing),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusChips(uiState: CompassUiState.Success) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (uiState.isLastKnownDirection) {
            TwoverseStatusChip(text = stringResource(R.string.compass_last_known_direction))
        }
        if (uiState.hasCompassSensor && uiState.isCalibrated) {
            TwoverseStatusChip(text = stringResource(R.string.compass_calibrated), dotColor = colors.gold)
        }
        uiState.partnerUpdatedAgo?.let { updatedAgo ->
            TwoverseStatusChip(text = stringResource(R.string.compass_partner_location, updatedAgo.shortText()))
        }
    }
}

/** Without a compass sensor the direction is shown as text (FR-CMP-8). */
@Composable
private fun NoCompassDirection(direction: CompassDirection?) {
    Text(
        text = direction?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.compass_dial_no_direction),
        style = MaterialTheme.typography.displaySmall,
        color = TwoverseTheme.colors.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TipCard(textRes: Int) {
    val colors = TwoverseTheme.colors
    val shape = TwoverseTheme.shapes.cardSmall
    val border = if (colors.isDark) Modifier.border(1.dp, colors.outline, shape) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(border)
            .background(colors.surfaceVariant, shape)
            .padding(horizontal = TwoverseTheme.spacing.md, vertical = TwoverseTheme.spacing.smd),
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(TipIconSize),
        )
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun CompassScreenPreview() {
    TwoverseTheme {
        CompassScreen(uiState = PreviewCompassState, needleRotation = { 42f })
    }
}

@PreviewLightDark
@Composable
private fun CompassScreenOutdatedPreview() {
    TwoverseTheme {
        CompassScreen(
            uiState = PreviewCompassState.copy(
                freshness = LocationFreshness.Outdated,
                partnerUpdatedAgo = ElapsedTime.Hours(3),
                isCalibrated = false,
            ),
            needleRotation = { 42f },
        )
    }
}

@PreviewLightDark
@Composable
private fun CompassScreenNoSensorPreview() {
    TwoverseTheme {
        CompassScreen(
            uiState = PreviewCompassState.copy(hasCompassSensor = false, showsNeedle = false),
            needleRotation = { null },
        )
    }
}

@PreviewLightDark
@Composable
private fun CompassScreenUnavailablePreview() {
    TwoverseTheme {
        CompassScreen(
            uiState = PreviewCompassState.copy(
                distance = null,
                direction = null,
                bearingDegrees = null,
                showsNeedle = false,
                freshness = LocationFreshness.Unavailable,
                partnerUpdatedAgo = null,
                unavailableReason = LocationUnavailableReason.PartnerUnavailable,
            ),
            needleRotation = { null },
        )
    }
}

private val PreviewCompassState = CompassUiState.Success(
    distance = "94.6",
    distanceUnit = DistanceUnit.Kilometres,
    direction = CompassDirection.NorthEast,
    bearingDegrees = 42,
    showsNeedle = true,
    hasCompassSensor = true,
    isPointingAtPartner = false,
    freshness = LocationFreshness.Live,
    partnerUpdatedAgo = ElapsedTime.Seconds(12),
    unavailableReason = null,
    isCalibrated = true,
)
