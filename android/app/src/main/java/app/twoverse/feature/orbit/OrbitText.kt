package app.twoverse.feature.orbit

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.twoverse.R
import app.twoverse.core.common.formatDate
import app.twoverse.core.model.Meetup
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

// Display text shared by the Our Orbit screens.

/** "2 years, 3 months, 5 days", leaving out zero parts; "Your first day" on day 1. */
@Composable
internal fun periodText(period: Period): String {
    val parts = listOfNotNull(
        period.years.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.orbit_years, it, it) },
        period.months.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.orbit_months, it, it) },
        period.days.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.orbit_days, it, it) },
    )
    return if (parts.isEmpty()) stringResource(R.string.orbit_first_day) else parts.joinToString(stringResource(R.string.orbit_list_separator))
}

/** "22 June 2025" in the device locale's order. */
@Composable
internal fun longDate(date: LocalDate): String = formatDate(date, LongDatePattern, LocalConfiguration.current.locales[0])

/** "22 Jun 2025". */
@Composable
internal fun shortDate(date: LocalDate): String = formatDate(date, ShortDatePattern, LocalConfiguration.current.locales[0])

/** "Today", "Tomorrow" or "in 23 days". */
@Composable
internal fun inDaysText(days: Long): String = when (days) {
    0L -> stringResource(R.string.orbit_today)
    1L -> stringResource(R.string.orbit_tomorrow)
    else -> pluralStringResource(R.plurals.orbit_in_days, days.toInt(), days.toInt())
}

/** "10 Oct 2026", or "10 Oct 2026 – 14 Oct 2026" for a visit of several days. */
@Composable
internal fun meetupDates(meetup: Meetup): String {
    val start = shortDate(meetup.startDate)
    val end = meetup.endDate?.takeIf { it != meetup.startDate } ?: return start
    return stringResource(R.string.orbit_meetup_range, start, shortDate(end))
}

/** Both the first and last day count. */
internal val Meetup.lengthDays: Long get() = ChronoUnit.DAYS.between(startDate, lastDay) + 1

private const val LongDatePattern = "dMMMMy"
private const val ShortDatePattern = "dMMMy"
