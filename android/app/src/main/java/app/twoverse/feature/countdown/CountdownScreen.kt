package app.twoverse.feature.countdown

import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.core.common.dateLocale
import app.twoverse.R
import app.twoverse.core.common.CountdownTime
import app.twoverse.core.designsystem.component.GoldStar
import app.twoverse.core.designsystem.component.IconTile
import app.twoverse.core.designsystem.component.TwoverseBackButton
import app.twoverse.core.designsystem.component.TwoverseCard
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseSecondaryButton
import app.twoverse.core.designsystem.theme.TwoverseTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val UnitTileVerticalPadding = 12.dp
private val CardHorizontalPadding = 18.dp
private val PlanRowMinHeight = 58.dp
private val PlanIconTileSize = 36.dp
private val PlanIconSize = 18.dp
private const val DatePattern = "EEEEdMMMM"
private val CelebrationStarSize = 56.dp

/** Until We Meet (FR-CNT-1 to FR-CNT-7). */
@Composable
fun CountdownScreen(
    uiState: CountdownUiState,
    onBack: () -> Unit,
    onEditPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    val content = uiState.content

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
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    TwoverseBackButton(onClick = onBack)
                    Text(
                        text = stringResource(R.string.countdown_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                when (content) {
                    CountdownContent.Loading -> Unit
                    CountdownContent.NoDate -> NoDateMessage(afterReunion = false)
                    CountdownContent.AfterReunion -> NoDateMessage(afterReunion = true)
                    is CountdownContent.Celebrating -> CelebrationContent(content.plan)
                    is CountdownContent.Counting -> CountingContent(content)
                }
            }
            when (content) {
                CountdownContent.Loading -> Unit
                CountdownContent.NoDate, CountdownContent.AfterReunion -> TwoversePrimaryButton(
                    text = stringResource(R.string.countdown_set_date),
                    onClick = onEditPlan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.xl),
                )
                is CountdownContent.Counting, is CountdownContent.Celebrating -> Column(modifier = Modifier.padding(top = spacing.xl)) {
                    TwoverseSecondaryButton(
                        text = stringResource(R.string.countdown_change_date),
                        onClick = onEditPlan,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.countdown_shared_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.xs),
                    )
                }
            }
        }
    }

}

@Composable
private fun NoDateMessage(afterReunion: Boolean) {
    Text(
        text = stringResource(if (afterReunion) R.string.countdown_next_time else R.string.countdown_empty),
        style = MaterialTheme.typography.headlineMedium,
        color = TwoverseTheme.colors.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TwoverseTheme.spacing.xxl * 2),
    )
}

@Composable
private fun CountingContent(content: CountdownContent.Counting) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    val days = content.timeLeft.days
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = days.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = colors.onSurface,
        )
        Text(
            text = pluralStringResource(R.plurals.countdown_days_to_go, days.toInt()),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.xxs),
        )
    }
    UnitTiles(timeLeft = content.timeLeft, modifier = Modifier.padding(top = spacing.lg))
    GettingCloserCard(progress = content.progress, modifier = Modifier.padding(top = spacing.smd))
    PlanCard(plan = content.plan, modifier = Modifier.padding(top = spacing.smd))
}

/** The countdown reached zero on the reunion day (FR-CNT-5). */
@Composable
private fun CelebrationContent(plan: ReunionDetails) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        GoldStar(size = CelebrationStarSize)
        Text(
            text = stringResource(R.string.countdown_celebration_title),
            style = MaterialTheme.typography.displaySmall,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.countdown_celebration_body),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    PlanCard(plan = plan, modifier = Modifier.padding(top = spacing.xl))
}

@Composable
private fun UnitTiles(timeLeft: CountdownTime, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.sm),
    ) {
        UnitTile(value = timeLeft.hours, labelRes = R.string.countdown_hours, modifier = Modifier.weight(1f))
        UnitTile(value = timeLeft.minutes, labelRes = R.string.countdown_minutes, modifier = Modifier.weight(1f))
        UnitTile(value = timeLeft.seconds, labelRes = R.string.countdown_seconds, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun UnitTile(value: Int, @StringRes labelRes: Int, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    TwoverseCard(shape = TwoverseTheme.shapes.cardSmall, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = UnitTileVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = String.format(locale, "%02d", value),
                style = TwoverseTheme.textStyles.countdownUnitValue,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(labelRes).uppercase(locale),
                style = TwoverseTheme.textStyles.countdownUnitLabel,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GettingCloserCard(progress: Float, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    TwoverseCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = CardHorizontalPadding, vertical = TwoverseTheme.spacing.md)) {
            Text(
                text = stringResource(R.string.countdown_getting_closer),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface,
            )
            Text(
                text = stringResource(R.string.countdown_planets_meet),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            GettingCloserTrack(progress = progress, modifier = Modifier.padding(top = TwoverseTheme.spacing.sm))
        }
    }
}

@Composable
private fun PlanCard(plan: ReunionDetails, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val dateText = remember(plan.date, locale) { plan.date.format(dateFormatter(locale)) }
    val time = plan.time
    val rows = buildList {
        add(PlanRow(R.drawable.ic_calendar, R.string.countdown_date, dateText))
        plan.place?.let { add(PlanRow(R.drawable.ic_pin, R.string.countdown_place, it)) }
        if (time != null) {
            val timeText = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
            add(PlanRow(R.drawable.ic_clock, R.string.countdown_time, stringResource(R.string.countdown_time_around, timeText)))
        }
        plan.note?.let { add(PlanRow(R.drawable.ic_heart, R.string.countdown_note, it)) }
    }
    TwoverseCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = CardHorizontalPadding, vertical = TwoverseTheme.spacing.xxs),
        ) {
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = TwoverseTheme.colors.outline)
                PlanRowItem(row)
            }
        }
    }
}

private data class PlanRow(@param:DrawableRes val icon: Int, @param:StringRes val labelRes: Int, val value: String)

@Composable
private fun PlanRowItem(row: PlanRow) {
    val colors = TwoverseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PlanRowMinHeight)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.smd),
    ) {
        IconTile(icon = row.icon, size = PlanIconTileSize, iconSize = PlanIconSize)
        Column {
            Text(
                text = stringResource(row.labelRes),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                color = colors.onSurfaceVariant,
            )
            Text(
                text = row.value,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
            )
        }
    }
}

/** Locale-aware "Saturday, 10 October". */
private fun dateFormatter(locale: Locale): DateTimeFormatter {
    val formatLocale = dateLocale(locale)
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(formatLocale, DatePattern), formatLocale)
}

@PreviewLightDark
@Composable
private fun CountdownScreenPreview() {
    TwoverseTheme {
        CountdownScreen(
            uiState = CountdownUiState(
                content = CountdownContent.Counting(
                    timeLeft = CountdownTime(days = 12, hours = 8, minutes = 24, seconds = 10),
                    progress = 0.55f,
                    plan = PreviewPlan,
                ),
            ),
            onBack = {},
            onEditPlan = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun CountdownScreenNoDatePreview() {
    TwoverseTheme {
        CountdownScreen(
            uiState = CountdownUiState(content = CountdownContent.NoDate),
            onBack = {},
            onEditPlan = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun CountdownScreenCelebrationPreview() {
    TwoverseTheme {
        CountdownScreen(
            uiState = CountdownUiState(content = CountdownContent.Celebrating(PreviewPlan)),
            onBack = {},
            onEditPlan = {},
        )
    }
}

private val PreviewPlan = ReunionDetails(
    date = LocalDate.of(2026, 10, 10),
    time = LocalTime.of(10, 0),
    place = "Kandy",
    note = "Bring the camera",
)
