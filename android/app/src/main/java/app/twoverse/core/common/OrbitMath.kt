package app.twoverse.core.common

import app.twoverse.core.model.Meetup
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

// Our Orbit calculations (FR-ORB-4 to FR-ORB-7), on local dates. The start date is day 1:
// on the day the relationship began the couple has been together 1 day.

/** How long the couple has been together: the elapsed [period] and the day number ([totalDays]). */
data class TogetherDuration(val period: Period, val totalDays: Long)

/** Null before [since] (a date set in another time zone that hasn't begun here yet). */
fun togetherDuration(since: LocalDate, today: LocalDate): TogetherDuration? {
    if (today.isBefore(since)) return null
    return TogetherDuration(period = Period.between(since, today), totalDays = dayNumber(since, today))
}

/** Day 1 is [since] itself. */
fun dayNumber(since: LocalDate, date: LocalDate): Long = ChronoUnit.DAYS.between(since, date) + 1

/** The date of day [day]. */
fun dateOfDay(since: LocalDate, day: Long): LocalDate = since.plusDays(day - 1)

data class Anniversary(val years: Int, val date: LocalDate, val daysUntil: Long) {
    val isToday: Boolean get() = daysUntil == 0L
}

/**
 * The next anniversary on or after [today] (today itself counts). A 29 February anniversary falls
 * on 28 February in years without one, as [LocalDate.plusYears] does.
 */
fun nextAnniversary(since: LocalDate, today: LocalDate): Anniversary {
    var years = (today.year - since.year).coerceAtLeast(1)
    var date = since.plusYears(years.toLong())
    while (date.isBefore(today)) {
        years++
        date = since.plusYears(years.toLong())
    }
    return Anniversary(years = years, date = date, daysUntil = ChronoUnit.DAYS.between(today, date))
}

/** Something worth celebrating (FR-ORB-6). */
sealed interface Milestone {
    /** Day 100, 365, 500, 1000, then every 1000 days. */
    data class Days(val day: Long) : Milestone

    /** A yearly anniversary. */
    data class Years(val years: Int) : Milestone
}

data class UpcomingMilestone(val milestone: Milestone, val date: LocalDate, val daysUntil: Long) {
    val isToday: Boolean get() = daysUntil == 0L
}

private val FirstDayMilestones = listOf(100L, 365L, 500L, 1000L)
private const val MilestoneStep = 1000L

/** The first day milestone on or after day [day]. */
fun nextDayMilestone(day: Long): Long =
    FirstDayMilestones.firstOrNull { it >= day } ?: (((day + MilestoneStep - 1) / MilestoneStep) * MilestoneStep)

/** The next milestone on or after [today]; an anniversary wins when both fall on the same day. */
fun nextMilestone(since: LocalDate, today: LocalDate): UpcomingMilestone {
    val todayNumber = dayNumber(since, today).coerceAtLeast(1)
    val day = nextDayMilestone(todayNumber)
    val dayDate = dateOfDay(since, day)
    val anniversary = nextAnniversary(since, today)
    return if (!anniversary.date.isAfter(dayDate)) {
        UpcomingMilestone(Milestone.Years(anniversary.years), anniversary.date, anniversary.daysUntil)
    } else {
        UpcomingMilestone(Milestone.Days(day), dayDate, ChronoUnit.DAYS.between(today, dayDate))
    }
}

/** What the couple's meetups add up to (FR-ORB-7). */
data class MeetupStats(
    val timesMet: Int,
    /** Days spent together in person so far, both ends counted, overlaps once. */
    val daysTogether: Long,
    /** Days since they were last together; 0 while together today; null if they haven't met yet. */
    val daysSinceLastMet: Long?,
)

/** Meetups that start after [today] count as met but add no days yet. */
fun meetupStats(meetups: List<Meetup>, today: LocalDate): MeetupStats {
    val ranges = meetups
        .filter { !it.startDate.isAfter(today) }
        .map { it.startDate to minOf(it.lastDay, today) }
        .sortedBy { it.first }
    var total = 0L
    var current: Pair<LocalDate, LocalDate>? = null
    for (range in ranges) {
        val open = current
        current = when {
            open == null -> range
            // Overlapping or touching ranges merge, so shared days count once.
            !range.first.isAfter(open.second.plusDays(1)) -> open.first to maxOf(open.second, range.second)
            else -> {
                total += ChronoUnit.DAYS.between(open.first, open.second) + 1
                range
            }
        }
    }
    current?.let { total += ChronoUnit.DAYS.between(it.first, it.second) + 1 }
    val lastDay = ranges.maxOfOrNull { it.second }
    return MeetupStats(
        timesMet = meetups.size,
        daysTogether = total,
        daysSinceLastMet = lastDay?.let { ChronoUnit.DAYS.between(it, today) },
    )
}
