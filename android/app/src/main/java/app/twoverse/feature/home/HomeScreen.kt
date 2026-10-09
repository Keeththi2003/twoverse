package app.twoverse.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.text.partnerString
import app.twoverse.core.designsystem.text.partnerNameStart
import app.twoverse.core.designsystem.text.PronounStrings
import app.twoverse.core.common.formatDate
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
import app.twoverse.core.designsystem.component.TwoverseTonalButton
import app.twoverse.core.designsystem.text.labelRes
import app.twoverse.core.designsystem.text.longText
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DistanceUnit
import java.time.LocalDate

private val DistanceCardHorizontalPadding = 22.dp
private val TilePadding = 18.dp
private val OursRowVerticalPadding = 16.dp
private val FreshnessDotSize = 7.dp
private val ChevronSize = 20.dp
private val HintChevronSize = 16.dp

/**
 * Our Universe (FR-LOC-8 to FR-LOC-11, FR-CMP-4, FR-CNT-4, FR-ORB-9), with a way to send a
 * Shooting Star (FR-STAR-1). The distance card opens Your Star.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onOpenCompass: () -> Unit,
    onOpenCountdown: () -> Unit,
    onOpenVault: () -> Unit,
    onSendMemory: () -> Unit,
    onSendShootingStar: () -> Unit,
    onOpenLocationSetup: () -> Unit,
    modifier: Modifier = Modifier,
    orbitActions: HomeOrbitActions = HomeOrbitActions(),
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
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal)
                .padding(bottom = spacing.md),
        ) {
            HomeHeader(
                dayPeriod = uiState.dayPeriod,
                sharingStatus = uiState.sharingStatus,
                onOpenLocationSetup = onOpenLocationSetup,
            )
            uiState.meetupQuestion?.let { date ->
                Spacer(modifier = Modifier.height(spacing.md))
                MeetupQuestionCard(date = date, actions = orbitActions)
            }
            Spacer(modifier = Modifier.height(spacing.lg))
            DistanceCard(uiState = uiState, onClick = onOpenCompass)
            Spacer(modifier = Modifier.height(spacing.smd))
            // Intrinsic height plus fillMaxHeight keeps both tiles equally tall, whatever their text.
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing.smd),
            ) {
                CountdownTile(
                    daysUntilReunion = uiState.daysUntilReunion,
                    onClick = onOpenCountdown,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                TogetherTile(
                    orbit = uiState.orbit,
                    actions = orbitActions,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                TwoversePrimaryButton(
                    text = stringResource(R.string.home_send_memory),
                    onClick = onSendMemory,
                    leadingIcon = R.drawable.ic_image,
                    adaptive = true,
                    contentDescription = stringResource(R.string.home_send_memory_description),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                TwoverseTonalButton(
                    text = stringResource(R.string.home_send_star),
                    onClick = onSendShootingStar,
                    leadingIcon = R.drawable.ic_sparkle,
                    adaptive = true,
                    contentDescription = stringResource(R.string.home_send_star_description),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
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

/** Distance, freshness and her direction; the whole card opens Your Star (FR-CMP). */
@Composable
private fun DistanceCard(uiState: HomeUiState.Success, onClick: () -> Unit) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    TwoverseCard(
        shape = TwoverseTheme.shapes.cardLarge,
        onClick = onClick,
        onClickLabel = stringResource(R.string.home_distance_open_star),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
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
                    modifier = Modifier.weight(1f),
                )
                FreshnessBadge(freshness = uiState.freshness)
                YourStarHint(modifier = Modifier.padding(start = spacing.sm))
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
                PersonCity(name = stringResource(R.string.home_you), city = uiState.myCity, modifier = Modifier.weight(1f, fill = false))
                PersonCity(name = partnerNameStart(), city = uiState.partnerCity, modifier = Modifier.weight(1f, fill = false))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                UpdatedText(
                    freshness = uiState.freshness,
                    updatedAgo = uiState.partnerUpdatedAgo,
                    modifier = Modifier.weight(1f),
                )
                DirectionLabel(
                    direction = uiState.partnerDirection,
                    bearing = uiState.partnerBearing,
                    freshness = uiState.freshness,
                )
            }
        }
    }
}

/** "Your Star ›", so it is clear the card can be tapped. */
@Composable
private fun YourStarHint(modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.compass_title),
            style = MaterialTheme.typography.labelSmall,
            color = colors.primary,
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(HintChevronSize),
        )
    }
}

/**
 * The needle along her bearing, "She's north-west" and the degrees. Outdated positions say it is
 * the last known direction; without her position it is hidden (FR-CMP-6).
 */
@Composable
private fun DirectionLabel(direction: CompassDirection?, bearing: Int?, freshness: LocationFreshness) {
    if (direction == null || bearing == null || freshness == LocationFreshness.Unavailable) return
    val colors = TwoverseTheme.colors
    val name = stringResource(direction.labelRes()).lowercase(LocalConfiguration.current.locales[0])
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xxs),
    ) {
        DirectionNeedle(bearingDegrees = bearing.toFloat())
        Text(
            text = if (freshness == LocationFreshness.Outdated) {
                stringResource(R.string.home_direction_last_known, name)
            } else {
                partnerString(StarDirection, name)
            },
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurface,
        )
        Text(
            text = stringResource(R.string.home_direction_degrees, bearing),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
            color = colors.onSurfaceVariant,
        )
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
private fun PersonCity(name: String, city: String?, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
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
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
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

/**
 * Days together in the same large style as the countdown, "Together", and how often you met;
 * opens Our Orbit. Before the date is set it offers to set it (FR-ORB-2, FR-ORB-9).
 */
@Composable
private fun TogetherTile(orbit: HomeOrbit?, actions: HomeOrbitActions, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    HomeTile(onClick = if (orbit != null) actions.onOpenOrbit else actions.onSetTogetherSince, modifier = modifier) {
        IconTile(icon = R.drawable.ic_heart)
        Column {
            Text(
                text = if (orbit != null) {
                    pluralStringResource(R.plurals.home_countdown_days, orbit.totalDays.toInt(), orbit.totalDays)
                } else {
                    stringResource(R.string.home_together_set_date)
                },
                style = MaterialTheme.typography.headlineSmall.copy(lineHeight = MaterialTheme.typography.headlineSmall.fontSize),
                color = colors.onSurface,
            )
            Text(
                text = stringResource(R.string.home_together_label),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = TwoverseTheme.spacing.xxs),
            )
            if (orbit != null && orbit.timesMet > 0) {
                Text(
                    text = pluralStringResource(R.plurals.home_met_times, orbit.timesMet, orbit.timesMet),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = TwoverseTheme.spacing.xxs / 2),
                )
            }
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

/** Our Orbit on Our Universe (FR-ORB-2, FR-ORB-9, FR-ORB-10). */
data class HomeOrbitActions(
    val onOpenOrbit: () -> Unit = {},
    val onSetTogetherSince: () -> Unit = {},
    val onMeetupQuestionYes: () -> Unit = {},
    val onMeetupQuestionNo: () -> Unit = {},
)

/** "Did you meet on 10 October?" once a reunion has passed (FR-ORB-10). */
@Composable
private fun MeetupQuestionCard(date: LocalDate, actions: HomeOrbitActions) {
    val colors = TwoverseTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val day = formatDate(date, QuestionDatePattern, locale)
    TwoverseCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(TilePadding),
            verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.home_meetup_question, day),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(R.string.home_meetup_question_body),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                TwoversePrimaryButton(
                    text = stringResource(R.string.home_meetup_question_yes),
                    onClick = actions.onMeetupQuestionYes,
                    small = true,
                    modifier = Modifier.weight(1f),
                )
                TwoverseTextButton(
                    text = stringResource(R.string.home_meetup_question_no),
                    onClick = actions.onMeetupQuestionNo,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private const val QuestionDatePattern = "dMMMM"

/** "She's north-east", "He's north-east", "They're north-east". */
private val StarDirection = PronounStrings(R.string.home_star_direction_she, R.string.home_star_direction_he, R.string.home_star_direction_they)

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
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenOrbitPromptsPreview() {
    TwoverseTheme {
        HomeScreen(
            uiState = PreviewHomeState.copy(
                orbit = null,
                askTogetherSince = true,
                meetupQuestion = LocalDate.of(2026, 10, 10),
            ),
            onOpenCompass = {},
            onOpenCountdown = {},
            onOpenVault = {},
            onSendMemory = {},
            onSendShootingStar = {},
            onOpenLocationSetup = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenFarAndOutdatedPreview() {
    TwoverseTheme {
        HomeScreen(
            uiState = PreviewHomeState.copy(
                distance = "14,285",
                freshness = LocationFreshness.Outdated,
                partnerUpdatedAgo = ElapsedTime.Hours(3),
                partnerCity = "London",
                partnerDirection = CompassDirection.NorthWest,
                partnerBearing = 315,
                orbit = HomeOrbit(totalDays = 475, timesMet = 0),
            ),
            onOpenCompass = {},
            onOpenCountdown = {},
            onOpenVault = {},
            onSendMemory = {},
            onSendShootingStar = {},
            onOpenLocationSetup = {},
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
    partnerBearing = 42,
    daysUntilReunion = 12,
    memoryCount = 17,
    newMemoryCount = 2,
    orbit = HomeOrbit(totalDays = 845, timesMet = 7),
)
