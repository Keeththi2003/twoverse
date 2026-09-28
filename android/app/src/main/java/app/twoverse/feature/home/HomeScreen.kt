package app.twoverse.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.DayPeriod
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.common.LocationUnavailableReason
import app.twoverse.core.designsystem.component.IconTile
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseStatusChip
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.text.labelRes
import app.twoverse.core.designsystem.text.longText
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DistanceUnit

private val DistanceCardHorizontalPadding = 22.dp
private val TilePadding = 18.dp
private val OursRowVerticalPadding = 16.dp
private val FreshnessDotSize = 7.dp
private val ChevronSize = 20.dp

/** Our Universe (FR-LOC-8 to FR-LOC-11, FR-CNT-4), with a way to send a Shooting Star (FR-STAR-1). */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onOpenCompass: () -> Unit,
    onOpenCountdown: () -> Unit,
    onOpenVault: () -> Unit,
    onSendMemory: () -> Unit,
    onSendShootingStar: () -> Unit,
    onOpenLocationSetup: () -> Unit,
    miniNeedleRotation: () -> Float?,
    modifier: Modifier = Modifier,
) {
    val spacing = TwoverseTheme.spacing
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TwoverseTheme.colors.background),
    ) {
        if (uiState !is HomeUiState.Success) return@Box
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.md),
        ) {
            HomeHeader(
                dayPeriod = uiState.dayPeriod,
                sharingStatus = uiState.sharingStatus,
                onOpenLocationSetup = onOpenLocationSetup,
            )
            Spacer(modifier = Modifier.height(spacing.lg))
            DistanceCard(uiState = uiState)
            Spacer(modifier = Modifier.height(spacing.smd))
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing.smd),
            ) {
                YourStarTile(
                    direction = uiState.partnerDirection,
                    needleRotation = miniNeedleRotation,
                    onClick = onOpenCompass,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                CountdownTile(
                    daysUntilReunion = uiState.daysUntilReunion,
                    onClick = onOpenCountdown,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
            Spacer(modifier = Modifier.height(spacing.smd))
            OursRow(
                memoryCount = uiState.memoryCount,
                newMemoryCount = uiState.newMemoryCount,
                onClick = onOpenVault,
            )
            Spacer(modifier = Modifier.height(spacing.md))
            TwoversePrimaryButton(
                text = stringResource(R.string.home_send_memory),
                onClick = onSendMemory,
                leadingIcon = R.drawable.ic_plus,
                modifier = Modifier.fillMaxWidth(),
            )
            TwoverseTextButton(
                text = stringResource(R.string.home_send_star),
                onClick = onSendShootingStar,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HomeHeader(dayPeriod: DayPeriod, sharingStatus: SharingStatus, onOpenLocationSetup: () -> Unit) {
    val colors = TwoverseTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(dayPeriod.greetingRes()),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = colors.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        val isOn = sharingStatus == SharingStatus.On
        TwoverseStatusChip(
            text = stringResource(
                when (sharingStatus) {
                    SharingStatus.On -> R.string.home_location_on
                    SharingStatus.Off -> R.string.home_location_off
                    SharingStatus.PermissionNeeded -> R.string.home_location_permission_needed
                },
            ),
            dotColor = if (isOn) colors.gold else colors.onSurfaceVariant,
            modifier = if (isOn) {
                Modifier
            } else {
                Modifier
                    .minimumInteractiveComponentSize()
                    .clip(TwoverseTheme.shapes.circle)
                    .clickable(role = Role.Button, onClick = onOpenLocationSetup)
            },
        )
    }
}

@Composable
private fun DistanceCard(uiState: HomeUiState.Success) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    TwoverseCard(
        shape = TwoverseTheme.shapes.cardLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = DistanceCardHorizontalPadding, vertical = spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_distance_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                FreshnessBadge(freshness = uiState.freshness)
            }
            Spacer(modifier = Modifier.height(spacing.xs))
            DistanceValue(
                distance = uiState.distance,
                unit = uiState.distanceUnit,
                unavailableReason = uiState.unavailableReason,
            )
            DistanceArc(modifier = Modifier.padding(top = spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PersonCity(nameRes = R.string.home_you, city = uiState.myCity)
                PersonCity(nameRes = R.string.home_her, city = uiState.partnerCity)
            }
            UpdatedText(
                freshness = uiState.freshness,
                updatedAgo = uiState.partnerUpdatedAgo,
                modifier = Modifier.padding(top = spacing.sm),
            )
        }
    }
}

@Composable
private fun FreshnessBadge(freshness: LocationFreshness) {
    val colors = TwoverseTheme.colors
    val labelRes = when (freshness) {
        LocationFreshness.Live -> R.string.home_freshness_live
        LocationFreshness.Recent -> R.string.home_freshness_recent
        LocationFreshness.Outdated -> R.string.home_freshness_outdated
        LocationFreshness.Unavailable -> return
    }
    val isLive = freshness == LocationFreshness.Live
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(FreshnessDotSize)
                .background(if (isLive) colors.gold else colors.onSurfaceVariant, TwoverseTheme.shapes.circle),
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isLive) colors.goldText else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun DistanceValue(distance: String?, unit: DistanceUnit, unavailableReason: LocationUnavailableReason?) {
    val colors = TwoverseTheme.colors
    if (distance == null) {
        Column(verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xxs)) {
            Text(
                text = stringResource(R.string.home_distance_unavailable),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
            )
            if (unavailableReason != null) {
                Text(
                    text = stringResource(unavailableReason.messageRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        return
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xs),
    ) {
        Text(
            text = distance,
            style = MaterialTheme.typography.displayMedium,
            color = colors.onSurface,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = stringResource(
                when (unit) {
                    DistanceUnit.Kilometres -> R.string.home_km_apart
                    DistanceUnit.Miles -> R.string.home_miles_apart
                },
            ),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurfaceVariant,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
private fun PersonCity(nameRes: Int, city: String?) {
    val colors = TwoverseTheme.colors
    val name = stringResource(nameRes)
    val separator = stringResource(R.string.home_city_separator)
    Text(
        text = buildAnnotatedString {
            append(name)
            if (city != null) {
                withStyle(SpanStyle(color = colors.onSurfaceVariant, fontWeight = FontWeight.Medium)) {
                    append(separator)
                    append(city)
                }
            }
        },
        style = MaterialTheme.typography.labelMedium,
        color = colors.onSurface,
    )
}

@Composable
private fun UpdatedText(freshness: LocationFreshness, updatedAgo: ElapsedTime?, modifier: Modifier = Modifier) {
    if (updatedAgo == null) return
    val templateRes = when (freshness) {
        LocationFreshness.Live, LocationFreshness.Recent -> R.string.home_updated_ago
        LocationFreshness.Outdated -> R.string.home_last_seen
        LocationFreshness.Unavailable -> return
    }
    Text(
        text = stringResource(templateRes, updatedAgo.longText()),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
        color = TwoverseTheme.colors.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun YourStarTile(
    direction: CompassDirection?,
    needleRotation: () -> Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    HomeTile(onClick = onClick, modifier = modifier) {
        MiniCompass(
            needleRotation = needleRotation,
            modifier = Modifier.border(MiniCompassBorder, colors.outline, TwoverseTheme.shapes.circle),
        )
        Column {
            Text(
                text = stringResource(R.string.compass_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
            Text(
                text = if (direction != null) {
                    stringResource(
                        R.string.home_star_direction,
                        stringResource(direction.labelRes()).lowercase(LocalConfiguration.current.locales[0]),
                    )
                } else {
                    stringResource(R.string.home_direction_unavailable)
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = TwoverseTheme.spacing.xxs / 2),
            )
        }
    }
}

@Composable
private fun CountdownTile(daysUntilReunion: Long?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    HomeTile(onClick = onClick, modifier = modifier) {
        IconTile(icon = R.drawable.ic_calendar)
        Column {
            Text(
                text = if (daysUntilReunion != null) {
                    pluralStringResource(R.plurals.home_countdown_days, daysUntilReunion.toInt(), daysUntilReunion)
                } else {
                    stringResource(R.string.home_set_date)
                },
                style = MaterialTheme.typography.headlineSmall.copy(lineHeight = MaterialTheme.typography.headlineSmall.fontSize),
                color = colors.onSurface,
            )
            Text(
                text = stringResource(R.string.home_until_we_meet),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = TwoverseTheme.spacing.xxs),
            )
        }
    }
}

@Composable
private fun HomeTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TwoverseCard(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.padding(TilePadding),
            verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm),
        ) {
            content()
        }
    }
}

@Composable
private fun OursRow(memoryCount: Int, newMemoryCount: Int, onClick: () -> Unit) {
    val colors = TwoverseTheme.colors
    val count = pluralStringResource(R.plurals.home_memory_count, memoryCount, memoryCount)
    TwoverseCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = TilePadding, vertical = OursRowVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.smd),
        ) {
            IconTile(icon = R.drawable.ic_lock)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.vault_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = if (newMemoryCount > 0) {
                        pluralStringResource(R.plurals.home_memories_new, newMemoryCount, count, newMemoryCount)
                    } else {
                        count
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = TwoverseTheme.spacing.xxs / 2),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(ChevronSize),
            )
        }
    }
}

private fun DayPeriod.greetingRes(): Int = when (this) {
    DayPeriod.Morning -> R.string.home_greeting_morning
    DayPeriod.Afternoon -> R.string.home_greeting_afternoon
    DayPeriod.Evening -> R.string.home_greeting_evening
}

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    TwoverseTheme {
        HomeScreen(
            uiState = PreviewHomeState,
            onOpenCompass = {},
            onOpenCountdown = {},
            onOpenVault = {},
            onSendMemory = {},
            onSendShootingStar = {},
            onOpenLocationSetup = {},
            miniNeedleRotation = { 42f },
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenUnavailablePreview() {
    TwoverseTheme {
        HomeScreen(
            uiState = PreviewHomeState.copy(
                sharingStatus = SharingStatus.Off,
                distance = null,
                unavailableReason = LocationUnavailableReason.SharingOff,
                freshness = LocationFreshness.Unavailable,
                partnerUpdatedAgo = null,
                partnerDirection = null,
                daysUntilReunion = null,
                newMemoryCount = 0,
            ),
            onOpenCompass = {},
            onOpenCountdown = {},
            onOpenVault = {},
            onSendMemory = {},
            onSendShootingStar = {},
            onOpenLocationSetup = {},
            miniNeedleRotation = { 42f },
        )
    }
}

private val PreviewHomeState = HomeUiState.Success(
    dayPeriod = DayPeriod.Evening,
    sharingStatus = SharingStatus.On,
    distance = "94.6",
    distanceUnit = DistanceUnit.Kilometres,
    unavailableReason = null,
    freshness = LocationFreshness.Live,
    partnerUpdatedAgo = ElapsedTime.Seconds(12),
    myCity = "Colombo",
    partnerCity = "Kandy",
    partnerDirection = CompassDirection.NorthEast,
    daysUntilReunion = 12,
    memoryCount = 17,
    newMemoryCount = 2,
)
