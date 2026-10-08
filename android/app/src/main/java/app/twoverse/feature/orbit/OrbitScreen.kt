package app.twoverse.feature.orbit

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.common.Anniversary
import app.twoverse.core.common.MeetupStats
import app.twoverse.core.common.Milestone
import app.twoverse.core.common.TogetherDuration
import app.twoverse.core.common.UpcomingMilestone
import app.twoverse.core.designsystem.component.GoldStar
import app.twoverse.core.designsystem.component.OrbitGraphic
import app.twoverse.core.designsystem.component.OrbitPosition
import app.twoverse.core.designsystem.component.OrbitRing
import app.twoverse.core.designsystem.component.StarField
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoverseCircleIconButton
import app.twoverse.core.designsystem.component.TwoverseConfirmDialog
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.Meetup
import java.time.LocalDate
import java.time.Period

private val HeroOrbitSize = 150.dp
private val HeroHerSize = 30.dp
private val HeroYouSize = 22.dp
private val HeroStarSize = 24.dp
private val CelebrationStarSize = 30.dp
private val CelebrationHeight = 96.dp
private val TilePadding = 16.dp

/** Everything Our Orbit can ask for. */
data class OrbitActions(
    val onSetTogetherSince: () -> Unit = {},
    val onAddMeetup: () -> Unit = {},
    val onEditMeetup: (String) -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onDeleteConfirmed: () -> Unit = {},
    val onDeleteDismissed: () -> Unit = {},
)

/**
 * Our Orbit, a bottom-bar tab: how long you have been together, what's next, and the times you met
 * (FR-ORB-4 to FR-ORB-8, FR-ORB-11).
 */
@Composable
fun OrbitScreen(uiState: OrbitUiState, actions: OrbitActions, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenHorizontal)
            .padding(bottom = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = stringResource(R.string.orbit_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (uiState.isLoading) return@Column
        HeroCard(since = uiState.togetherSince, duration = uiState.duration, onSetTogetherSince = actions.onSetTogetherSince)
        uiState.celebration?.let { CelebrationCard(it) }
        val anniversary = uiState.nextAnniversary
        val milestone = uiState.nextDayMilestone
        if (anniversary != null && milestone != null) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing.smd),
            ) {
                AnniversaryTile(anniversary, Modifier.weight(1f).fillMaxHeight())
                MilestoneTile(milestone, Modifier.weight(1f).fillMaxHeight())
            }
        }
        StatsRow(uiState.stats)
        MeetupsSection(meetups = uiState.meetups, actions = actions)
        uiState.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
    if (uiState.pendingDeleteId != null) {
        TwoverseConfirmDialog(
            title = stringResource(R.string.orbit_meetup_delete_title),
            message = stringResource(R.string.orbit_meetup_delete_message),
            confirmLabel = stringResource(R.string.orbit_meetup_delete),
            onConfirm = actions.onDeleteConfirmed,
            onDismiss = actions.onDeleteDismissed,
            destructive = true,
        )
    }
}

/** "In orbit since …", the years, months and days, and the day count, around the orbit (FR-ORB-4). */
@Composable
private fun HeroCard(since: LocalDate?, duration: TogetherDuration?, onSetTogetherSince: () -> Unit) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    TwoverseCard(shape = TwoverseTheme.shapes.cardLarge, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            OrbitGraphic(
                size = HeroOrbitSize,
                her = OrbitPosition(angleDegrees = 40f),
                you = OrbitPosition(angleDegrees = 220f, ring = OrbitRing.Inner),
                herSize = HeroHerSize,
                youSize = HeroYouSize,
                starSize = HeroStarSize,
            )
            if (since == null || duration == null) {
                Text(
                    text = stringResource(R.string.orbit_not_set_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.orbit_not_set_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TwoversePrimaryButton(
                    text = stringResource(R.string.orbit_set_date),
                    onClick = onSetTogetherSince,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.xs),
                )
                return@Column
            }
            Text(
                text = stringResource(R.string.orbit_since, longDate(since)),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = periodText(duration.period),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = pluralStringResource(R.plurals.orbit_total_days, duration.totalDays.toInt(), duration.totalDays.toInt()),
                style = MaterialTheme.typography.titleSmall,
                color = colors.goldText,
            )
            TwoverseTextButton(text = stringResource(R.string.orbit_change_date), onClick = onSetTogetherSince)
        }
    }
}

/** A little starry moment on an anniversary or milestone day (FR-ORB-11). */
@Composable
private fun CelebrationCard(celebration: Celebration) {
    val colors = TwoverseTheme.colors
    val (title, body) = when (celebration) {
        is Celebration.Anniversary -> stringResource(R.string.orbit_celebrate_anniversary_title) to
            pluralStringResource(R.plurals.orbit_celebrate_anniversary_body, celebration.years, celebration.years)
        is Celebration.DayMilestone -> stringResource(R.string.orbit_celebrate_milestone_title, celebration.day.toInt()) to
            pluralStringResource(R.plurals.orbit_celebrate_milestone_body, celebration.day.toInt(), celebration.day.toInt())
    }
    TwoverseCard(color = colors.chip, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CelebrationHeight)
                .clip(TwoverseTheme.shapes.card),
            contentAlignment = Alignment.CenterStart,
        ) {
            StarField(modifier = Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .padding(horizontal = TwoverseTheme.spacing.lg)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.md),
            ) {
                GoldStar(size = CelebrationStarSize)
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Text(text = body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AnniversaryTile(anniversary: Anniversary, modifier: Modifier = Modifier) {
    InfoTile(
        label = stringResource(R.string.orbit_next_anniversary),
        value = inDaysText(anniversary.daysUntil),
        caption = stringResource(
            R.string.orbit_anniversary_caption,
            pluralStringResource(R.plurals.orbit_years, anniversary.years, anniversary.years),
            shortDate(anniversary.date),
        ),
        modifier = modifier,
    )
}

/** "1000 days in 23 days" (FR-ORB-6). */
@Composable
private fun MilestoneTile(milestone: UpcomingMilestone, modifier: Modifier = Modifier) {
    val day = (milestone.milestone as? Milestone.Days)?.day?.toInt() ?: return
    InfoTile(
        label = stringResource(R.string.orbit_next_milestone),
        value = pluralStringResource(R.plurals.orbit_days_count, day, day),
        caption = inDaysText(milestone.daysUntil),
        modifier = modifier,
    )
}

/** Times met, days together in person and when you last met (FR-ORB-7). */
@Composable
private fun StatsRow(stats: MeetupStats) {
    val lastMet = when (val days = stats.daysSinceLastMet) {
        null -> stringResource(R.string.orbit_last_met_never)
        0L -> stringResource(R.string.orbit_today)
        else -> pluralStringResource(R.plurals.orbit_days_ago, days.toInt(), days.toInt())
    }
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm),
    ) {
        InfoTile(
            label = pluralStringResource(R.plurals.orbit_times_met_label, stats.timesMet),
            value = stats.timesMet.toString(),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        InfoTile(
            label = pluralStringResource(R.plurals.orbit_days_in_person_label, stats.daysTogether.toInt()),
            value = stats.daysTogether.toString(),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        InfoTile(
            label = stringResource(R.string.orbit_last_met_label),
            value = lastMet,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier = Modifier, caption: String? = null) {
    val colors = TwoverseTheme.colors
    TwoverseCard(shape = TwoverseTheme.shapes.cardSmall, modifier = modifier) {
        Column(modifier = Modifier.padding(TilePadding), verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xxs)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            caption?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
    }
}

/** The meetup timeline, newest first, with add, edit and delete (FR-ORB-8). */
@Composable
private fun MeetupsSection(meetups: List<Meetup>, actions: OrbitActions) {
    val spacing = TwoverseTheme.spacing
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionHeader(R.string.orbit_meetups, Modifier.weight(1f))
        TwoverseTextButton(text = stringResource(R.string.orbit_add_meetup), onClick = actions.onAddMeetup)
    }
    if (meetups.isEmpty()) {
        Text(
            text = stringResource(R.string.orbit_meetups_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = TwoverseTheme.colors.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        meetups.forEach { meetup ->
            MeetupCard(
                meetup = meetup,
                onEdit = { actions.onEditMeetup(meetup.id) },
                onDelete = { actions.onDelete(meetup.id) },
            )
        }
    }
}

@Composable
private fun MeetupCard(meetup: Meetup, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = TwoverseTheme.colors
    TwoverseCard(shape = TwoverseTheme.shapes.cardSmall, onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = TilePadding, top = TwoverseTheme.spacing.sm, bottom = TwoverseTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xxs)) {
                Text(text = meetupDates(meetup), style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                val days = meetup.lengthDays.toInt()
                val details = listOfNotNull(pluralStringResource(R.plurals.orbit_days_count, days, days), meetup.place)
                Text(
                    text = details.joinToString(stringResource(R.string.orbit_detail_separator)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                meetup.note?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            }
            TwoverseCircleIconButton(
                icon = R.drawable.ic_trash,
                contentDescription = stringResource(R.string.orbit_meetup_delete_description, meetupDates(meetup)),
                onClick = onDelete,
                elevated = false,
            )
        }
    }
}

@Composable
private fun SectionHeader(@StringRes text: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(text).uppercase(LocalConfiguration.current.locales[0]),
        style = TwoverseTheme.textStyles.sectionHeader,
        color = TwoverseTheme.colors.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 2)
private val PreviewSince: LocalDate = LocalDate.of(2024, 2, 14)

private fun previewMeetup(id: Int, start: LocalDate, days: Long, place: String?, note: String? = null) =
    Meetup(id = "meetup-$id", startDate = start, endDate = start.plusDays(days - 1).takeIf { days > 1 }, place = place, note = note)

private val PreviewMeetups = listOf(
    previewMeetup(1, LocalDate.of(2026, 9, 20), 3, "Kandy", "Tea country and too much cake"),
    previewMeetup(2, LocalDate.of(2026, 7, 4), 1, "Colombo"),
    previewMeetup(3, LocalDate.of(2026, 4, 12), 6, "Galle"),
)

private fun previewState(meetups: List<Meetup> = PreviewMeetups) = OrbitUiState(
    isLoading = false,
    togetherSince = PreviewSince,
    duration = TogetherDuration(Period.between(PreviewSince, PreviewToday), totalDays = 962),
    nextAnniversary = Anniversary(years = 3, date = LocalDate.of(2027, 2, 14), daysUntil = 135),
    nextDayMilestone = UpcomingMilestone(Milestone.Days(1000), LocalDate.of(2026, 11, 9), daysUntil = 38),
    stats = MeetupStats(timesMet = meetups.size, daysTogether = 10, daysSinceLastMet = 10),
    meetups = meetups,
)

@PreviewLightDark
@Composable
private fun OrbitScreenPreview() {
    TwoverseTheme { OrbitScreen(uiState = previewState(), actions = OrbitActions()) }
}

@PreviewLightDark
@Composable
private fun OrbitScreenNotSetPreview() {
    TwoverseTheme {
        OrbitScreen(uiState = OrbitUiState(isLoading = false), actions = OrbitActions())
    }
}

@PreviewLightDark
@Composable
private fun OrbitScreenAnniversaryPreview() {
    TwoverseTheme {
        OrbitScreen(
            uiState = previewState().copy(
                celebration = Celebration.Anniversary(years = 2),
                nextAnniversary = Anniversary(years = 2, date = PreviewToday, daysUntil = 0),
            ),
            actions = OrbitActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun OrbitScreenManyMeetupsPreview() {
    val many = (0 until 12).map { previewMeetup(it, PreviewToday.minusDays(20L * it + 3), (it % 4 + 1).toLong(), "Visit ${it + 1}") }
    TwoverseTheme {
        OrbitScreen(
            uiState = previewState(many).copy(celebration = Celebration.DayMilestone(day = 1000)),
            actions = OrbitActions(),
        )
    }
}
