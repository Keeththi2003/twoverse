package app.twoverse.widget

import android.content.Context
import app.twoverse.core.common.formatDate
import app.twoverse.R
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.common.PartnerName
import app.twoverse.core.designsystem.text.PronounStrings
import app.twoverse.core.designsystem.text.resFor
import app.twoverse.core.designsystem.text.labelRes
import app.twoverse.core.model.DistanceUnit
import java.time.LocalDate
import java.util.Locale

// Widget text, built with a Context because Glance has no Compose string resources.

/** "Live", "Updated 5 min ago" or "Last seen 3 h ago"; [short] drops "Updated" for the small widget. */
internal fun Context.freshnessText(freshness: LocationFreshness, updatedAgo: ElapsedTime?, short: Boolean): String? {
    val ago = updatedAgo?.let(::shortAgo)
    return when (freshness) {
        LocationFreshness.Live -> getString(R.string.home_freshness_live)
        LocationFreshness.Recent -> ago?.let { if (short) it else getString(R.string.widget_updated, it) }
        LocationFreshness.Outdated -> ago?.let { getString(R.string.widget_last_seen, it) }
        LocationFreshness.Unavailable -> null
    }
}

private fun Context.shortAgo(elapsed: ElapsedTime): String {
    val count = elapsed.amount.toInt()
    return when (elapsed) {
        is ElapsedTime.Seconds -> getString(R.string.time_short_seconds, count)
        is ElapsedTime.Minutes -> getString(R.string.time_short_minutes, count)
        is ElapsedTime.Hours -> getString(R.string.time_short_hours, count)
        is ElapsedTime.Days -> getString(R.string.time_short_days, count)
    }
}

internal fun Context.unitShort(unit: DistanceUnit): String =
    getString(if (unit == DistanceUnit.Miles) R.string.unit_miles else R.string.unit_km)

internal fun Context.unitApart(unit: DistanceUnit): String =
    getString(if (unit == DistanceUnit.Miles) R.string.home_miles_apart else R.string.home_km_apart)

/** Short explanation when the distance can't be shown (FR-WGT-6). */
internal fun Context.locationMessage(location: WidgetLocation): String? = when (location) {
    WidgetLocation.NotPaired -> getString(R.string.widget_not_paired)
    WidgetLocation.SharingOff -> getString(R.string.widget_sharing_off)
    WidgetLocation.Unavailable -> getString(R.string.home_distance_unavailable)
    WidgetLocation.Available -> null
}

/** "She's north-east", "He's north-east" or "They're north-east", for the partner's pronouns (FR-PRO-2). */
internal fun Context.directionText(state: WidgetState): String? = state.direction?.let {
    getString(StarDirection.resFor(state.partner?.pronouns), getString(it.labelRes()).lowercase(locale()))
}

/** The partner's name, or "your partner" (FR-PRO-1). */
internal fun Context.partnerLabel(partner: PartnerName?): String = partner?.name ?: getString(R.string.partner_fallback_name)

/** "Ammu", or "Your partner" at the start of a label. */
internal fun Context.partnerLabelStart(partner: PartnerName?): String = partner?.name ?: getString(R.string.partner_fallback_name_start)

private val StarDirection = PronounStrings(R.string.home_star_direction_she, R.string.home_star_direction_he, R.string.home_star_direction_they)

internal fun Context.daysUntilText(reunion: WidgetReunion): String = if (reunion.isToday) {
    getString(R.string.widget_reunion_today)
} else {
    resources.getQuantityString(R.plurals.widget_days_until, reunion.daysUntil.toInt(), reunion.daysUntil)
}

internal fun Context.daysLabel(days: Long): String = resources.getQuantityString(R.plurals.widget_days_label, days.toInt())

/** "Fri, 16 Oct" in the device locale's order and words. */
internal fun Context.reunionDate(date: LocalDate): String = formatDate(date, ReunionDatePattern, locale())

/** The small line above the Together card: "6 days until we meet · Fri, 16 Oct", or "Today's the day ✨". */
internal fun Context.reunionLineText(reunion: WidgetReunion): String = if (reunion.isToday) {
    getString(R.string.widget_reunion_today)
} else {
    getString(R.string.widget_reunion_line, daysUntilText(reunion), reunionDate(reunion.date))
}

/** "Since 22 Jun 2025". */
internal fun Context.sinceText(since: LocalDate): String = getString(R.string.widget_since, formatDate(since, SinceDatePattern, locale()))

/** "25 days to 500", or "Day 500 ✨" on the milestone itself. */
internal fun Context.milestoneText(orbit: WidgetOrbit): String = if (orbit.daysToMilestone == 0L) {
    getString(R.string.widget_milestone_today, orbit.nextMilestone.toInt())
} else {
    resources.getQuantityString(
        R.plurals.widget_days_to_milestone,
        orbit.daysToMilestone.toInt(),
        orbit.daysToMilestone.toInt(),
        orbit.nextMilestone.toInt(),
    )
}

/** "845 days together". */
internal fun Context.daysTogetherText(days: Long): String =
    resources.getQuantityString(R.plurals.widget_days_together, days.toInt(), days.toInt())

/** "days together", next to the number in large type. */
internal fun Context.daysTogetherLabel(days: Long): String = resources.getQuantityString(R.plurals.widget_days_together_label, days.toInt())

/** "1y 3m 17d · Met 2 times" (FR-WGT-8). */
internal fun Context.orbitSummary(orbit: WidgetOrbit): String {
    val period = orbit.period
    val parts = listOfNotNull(
        period.years.takeIf { it > 0 }?.let { getString(R.string.widget_years_short, it) },
        period.months.takeIf { it > 0 }?.let { getString(R.string.widget_months_short, it) },
        period.days.takeIf { it > 0 }?.let { getString(R.string.widget_days_short, it) },
    )
    val duration = if (parts.isEmpty()) getString(R.string.widget_first_day) else parts.joinToString(" ")
    val met = orbit.timesMet.takeIf { it > 0 }?.let { resources.getQuantityString(R.plurals.widget_met_times, it, it) }
    return listOfNotNull(duration, met).joinToString(" · ")
}

internal fun Context.newMemoriesText(count: Int): String =
    resources.getQuantityString(R.plurals.widget_new_memories, count, count)

private fun Context.locale(): Locale = resources.configuration.locales[0]

private const val ReunionDatePattern = "EEEdMMM"
private const val SinceDatePattern = "dMMMy"
