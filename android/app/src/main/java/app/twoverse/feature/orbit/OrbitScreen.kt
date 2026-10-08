package app.twoverse.feature.orbit

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.common.Anniversary
import app.twoverse.core.common.MeetupStats
import app.twoverse.core.common.Milestone
import app.twoverse.core.common.TogetherDuration
import app.twoverse.core.common.UpcomingMilestone
import app.twoverse.core.designsystem.component.GoldStar
import app.twoverse.core.designsystem.component.IconTile
import app.twoverse.core.designsystem.component.OrbitGraphic
import app.twoverse.core.designsystem.component.OrbitPosition
import app.twoverse.core.designsystem.component.OrbitRing
import app.twoverse.core.designsystem.component.Planet
import app.twoverse.core.designsystem.component.PlanetKind
import app.twoverse.core.designsystem.component.StarField
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoverseCircleIconButton
import app.twoverse.core.designsystem.component.TwoverseConfirmDialog
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseSecondaryButton
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.Meetup
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Period

private val HeroOrbitSize = 150.dp
private val HeroHerSize = 30.dp
private val HeroYouSize = 22.dp
private val HeroStarSize = 24.dp
private val CelebrationStarSize = 30.dp
private val CelebrationHeight = 96.dp
private val TilePadding = 16.dp
private val TileIconSize = 36.dp
private val TileIconGlyphSize = 18.dp
private val StatIconSize = 20.dp
private val ProgressHeight = 4.dp
private val EmptyYouSize = 44.dp
private val EmptyHerSize = 54.dp
private val SectionHeaderInset = 4.dp

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
                MilestoneTile(milestone, uiState.milestoneProgress, Modifier.weight(1f).fillMaxHeight())
            }
        }
        if (uiState.meetups.isEmpty()) {
            MeetupsHeader(showAdd = false, onAdd = actions.onAddMeetup)
            EmptyMeetupsCard(onAdd = actions.onAddMeetup)
        } else {
            StatsCard(uiState.stats)
            MeetupsHeader(showAdd = true, onAdd = actions.onAddMeetup)
            MeetupList(meetups = uiState.meetups, actions = actions)
        }
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

/**
 * The days together are the star: the slowly turning orbit, the day count large, then the years,
 * months and days, and when it began (FR-ORB-4). The pencil changes the date.
 */
@Composable
private fun HeroCard(since: LocalDate?, duration: TogetherDuration?, onSetTogetherSince: () -> Unit) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    TwoverseCard(shape = TwoverseTheme.shapes.cardLarge, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OrbitGraphic(
                    size = HeroOrbitSize,
                    her = OrbitPosition(angleDegrees = 40f),
                    you = OrbitPosition(angleDegrees = 220f, ring = OrbitRing.Inner),
                    herSize = HeroHerSize,
                    youSize = HeroYouSize,
                    starSize = HeroStarSize,
                    animated = true,
                )
                if (since == null || duration == null) {
                    NotSetContent(onSetTogetherSince)
                    return@Column
                }
                val locale = LocalConfiguration.current.locales[0]
                val days = duration.totalDays.toInt()
                Column(
                    modifier = Modifier
                        .padding(top = spacing.md)
                        .semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Primary/rose on the card surface passes WCAG AA for large text in both themes.
                    Text(
                        text = NumberFormat.getIntegerInstance(locale).format(duration.totalDays),
                        style = MaterialTheme.typography.displayMedium,
                        color = colors.primary,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.orbit_days_together_label, days),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                }
                Text(
                    text = periodText(duration.period),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = spacing.sm),
                )
                Text(
                    text = stringResource(R.string.orbit_since, longDate(since)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = spacing.xxs),
                )
            }
            if (since != null && duration != null) {
                TwoverseCircleIconButton(
                    icon = R.drawable.ic_edit,
                    contentDescription = stringResource(R.string.orbit_change_date_description),
                    onClick = onSetTogetherSince,
                    elevated = false,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(spacing.xxs),
                )
            }
        }
    }
}

@Composable
private fun NotSetContent(onSetTogetherSince: () -> Unit) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    Column(
        modifier = Modifier.padding(top = spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
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

/** "Next anniversary", "256 days", "2 years · 22 June 2027" (FR-ORB-5). */
@Composable
private fun AnniversaryTile(anniversary: Anniversary, modifier: Modifier = Modifier) {
    CountdownCard(
        icon = R.drawable.ic_calendar,
        label = stringResource(R.string.orbit_next_anniversary),
        value = daysAwayText(anniversary.daysUntil),
        detail = stringResource(
            R.string.orbit_anniversary_caption,
            pluralStringResource(R.plurals.orbit_years, anniversary.years, anniversary.years),
            longDate(anniversary.date),
        ),
        modifier = modifier,
    )
}

/** "Next milestone", "500 days", "in 25 days" and a line from today to it (FR-ORB-6). */
@Composable
private fun MilestoneTile(milestone: UpcomingMilestone, progress: MilestoneProgress?, modifier: Modifier = Modifier) {
    val day = (milestone.milestone as? Milestone.Days)?.day?.toInt() ?: return
    CountdownCard(
        icon = R.drawable.ic_flag,
        label = stringResource(R.string.orbit_next_milestone),
        value = pluralStringResource(R.plurals.orbit_days_count, day, day),
        detail = inDaysText(milestone.daysUntil),
        modifier = modifier,
    ) {
        if (progress != null) {
            val description = stringResource(R.string.orbit_milestone_progress, progress.day.toInt(), progress.milestone.toInt())
            LinearProgressIndicator(
                progress = { progress.fraction },
                color = TwoverseTheme.colors.primary,
                trackColor = TwoverseTheme.colors.outline,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwoverseTheme.spacing.xs)
                    .height(ProgressHeight)
                    .semantics { contentDescription = description },
            )
        }
    }
}

/** "Today", "1 day" or "256 days" away. */
@Composable
private fun daysAwayText(days: Long): String =
    if (days == 0L) stringResource(R.string.orbit_today) else pluralStringResource(R.plurals.orbit_days_count, days.toInt(), days.toInt())

@Composable
private fun CountdownCard(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
) {
    val colors = TwoverseTheme.colors
    TwoverseCard(shape = TwoverseTheme.shapes.cardSmall, modifier = modifier) {
        Column(
            modifier = Modifier.padding(TilePadding),
            verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xxs),
        ) {
            IconTile(icon = icon, size = TileIconSize, iconSize = TileIconGlyphSize)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = TwoverseTheme.spacing.xs),
            )
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
            Text(text = detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            extra()
        }
    }
}

/** Times met, days together in person and when you last met, in one card (FR-ORB-7). */
@Composable
private fun StatsCard(stats: MeetupStats) {
    val colors = TwoverseTheme.colors
    val lastMet = when (val days = stats.daysSinceLastMet) {
        null -> stringResource(R.string.orbit_last_met_never)
        0L -> stringResource(R.string.orbit_today)
        else -> pluralStringResource(R.plurals.orbit_days_ago, days.toInt(), days.toInt())
    }
    TwoverseCard(shape = TwoverseTheme.shapes.cardSmall, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            StatColumn(
                icon = R.drawable.ic_pin,
                value = stats.timesMet.toString(),
                label = stringResource(R.string.orbit_stat_times_met),
                description = pluralStringResource(R.plurals.orbit_stat_times_met_description, stats.timesMet, stats.timesMet),
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(color = colors.outline, modifier = Modifier.padding(vertical = TilePadding))
            StatColumn(
                icon = R.drawable.ic_calendar,
                value = stats.daysTogether.toString(),
                label = stringResource(R.string.orbit_stat_days_together),
                description = pluralStringResource(
                    R.plurals.orbit_stat_days_together_description,
                    stats.daysTogether.toInt(),
                    stats.daysTogether.toInt(),
                ),
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(color = colors.outline, modifier = Modifier.padding(vertical = TilePadding))
            StatColumn(
                icon = R.drawable.ic_clock,
                value = lastMet,
                label = stringResource(R.string.orbit_last_met_label),
                description = stringResource(R.string.orbit_stat_last_met_description, lastMet),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Icon, value and label. Values are one line, shrinking to fit ("12 days ago"), so all three sit on
 * the same baseline; labels below may take two lines.
 */
@Composable
private fun StatColumn(@DrawableRes icon: Int, value: String, label: String, description: String, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    val valueStyle = MaterialTheme.typography.headlineSmall
    Column(
        modifier = modifier
            .padding(horizontal = TwoverseTheme.spacing.xs, vertical = TilePadding)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.xxs),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(StatIconSize),
        )
        BasicText(
            text = value,
            style = valueStyle.copy(color = colors.onSurface, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = MaterialTheme.typography.labelLarge.fontSize,
                maxFontSize = valueStyle.fontSize,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** "MEETUPS" in the Settings section style, with a small Add button. */
@Composable
private fun MeetupsHeader(showAdd: Boolean, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TwoverseTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeader(R.string.orbit_meetups, Modifier.weight(1f))
        if (showAdd) {
            TwoverseSecondaryButton(
                text = stringResource(R.string.orbit_add_meetup_short),
                onClick = onAdd,
                leadingIcon = R.drawable.ic_plus,
                small = true,
            )
        }
    }
}

/** With no meetups yet: two planets touching, a friendly line and one clear action (FR-ORB-8). */
@Composable
private fun EmptyMeetupsCard(onAdd: () -> Unit) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    TwoverseCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Your planet's edge meets hers: lavender left, rose right, as on Home.
                Planet(kind = PlanetKind.You, size = EmptyYouSize, elevated = true, modifier = Modifier.offset(x = -EmptyYouSize / 2))
                Planet(kind = PlanetKind.Her, size = EmptyHerSize, elevated = true, modifier = Modifier.offset(x = EmptyHerSize / 2))
            }
            Text(
                text = stringResource(R.string.orbit_meetups_empty),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
            )
            TwoversePrimaryButton(
                text = stringResource(R.string.orbit_add_first_meetup),
                onClick = onAdd,
                leadingIcon = R.drawable.ic_plus,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The meetup timeline, newest first; tap to edit, or delete (FR-ORB-8). */
@Composable
private fun MeetupList(meetups: List<Meetup>, actions: OrbitActions) {
    Column(verticalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm)) {
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
    val spacing = TwoverseTheme.spacing
    TwoverseCard(
        shape = TwoverseTheme.shapes.cardSmall,
        onClick = onEdit,
        onClickLabel = stringResource(R.string.orbit_meetup_edit_title),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = TilePadding, top = spacing.sm, bottom = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.smd),
        ) {
            IconTile(icon = R.drawable.ic_pin, size = TileIconSize, iconSize = TileIconGlyphSize)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs / 2)) {
                Text(text = meetupDates(meetup), style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                val days = meetup.lengthDays.toInt()
                val details = listOfNotNull(meetup.place, pluralStringResource(R.plurals.orbit_days_count, days, days))
                Text(
                    text = details.joinToString(stringResource(R.string.orbit_detail_separator)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                meetup.note?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
        modifier = modifier
            .padding(horizontal = SectionHeaderInset)
            .semantics { heading() },
    )
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 9)
private val PreviewSince: LocalDate = LocalDate.of(2025, 6, 22)

private fun previewMeetup(id: Int, start: LocalDate, days: Long, place: String?, note: String? = null) =
    Meetup(id = "meetup-$id", startDate = start, endDate = start.plusDays(days - 1).takeIf { days > 1 }, place = place, note = note)

private val PreviewMeetups = listOf(
    previewMeetup(1, LocalDate.of(2026, 9, 20), 3, "Kandy", "Tea country, misty mornings and far too much cake at the hotel"),
    previewMeetup(2, LocalDate.of(2026, 7, 4), 1, "Colombo"),
    previewMeetup(3, LocalDate.of(2026, 4, 12), 6, "Galle", "Sunset on the fort walls"),
)

private fun previewState(meetups: List<Meetup> = PreviewMeetups) = OrbitUiState(
    isLoading = false,
    togetherSince = PreviewSince,
    duration = TogetherDuration(Period.between(PreviewSince, PreviewToday), totalDays = 475),
    nextAnniversary = Anniversary(years = 2, date = LocalDate.of(2027, 6, 22), daysUntil = 256),
    nextDayMilestone = UpcomingMilestone(Milestone.Days(500), LocalDate.of(2026, 11, 3), daysUntil = 25),
    stats = MeetupStats(timesMet = meetups.size, daysTogether = 10, daysSinceLastMet = if (meetups.isEmpty()) null else 12),
    meetups = meetups,
)

@PreviewLightDark
@Composable
private fun OrbitScreenSeveralMeetupsPreview() {
    TwoverseTheme { OrbitScreen(uiState = previewState(), actions = OrbitActions()) }
}

@PreviewLightDark
@Composable
private fun OrbitScreenNoMeetupsPreview() {
    TwoverseTheme { OrbitScreen(uiState = previewState(emptyList()), actions = OrbitActions()) }
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
private fun OrbitScreenAnniversaryDayPreview() {
    TwoverseTheme {
        OrbitScreen(
            uiState = previewState().copy(
                duration = TogetherDuration(Period.ofYears(1), totalDays = 366),
                celebration = Celebration.Anniversary(years = 1),
                nextAnniversary = Anniversary(years = 1, date = PreviewToday, daysUntil = 0),
            ),
            actions = OrbitActions(),
        )
    }
}

@PreviewLightDark
@Composable
private fun OrbitScreenMilestoneDayPreview() {
    TwoverseTheme {
        OrbitScreen(
            uiState = previewState().copy(
                duration = TogetherDuration(Period.of(1, 4, 13), totalDays = 500),
                celebration = Celebration.DayMilestone(day = 500),
                nextDayMilestone = UpcomingMilestone(Milestone.Days(500), PreviewToday, daysUntil = 0),
            ),
            actions = OrbitActions(),
        )
    }
}
