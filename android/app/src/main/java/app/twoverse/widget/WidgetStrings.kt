package app.twoverse.widget

import android.content.Context
import android.text.format.DateFormat
import app.twoverse.R
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.designsystem.text.labelRes
import app.twoverse.core.model.DistanceUnit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

/** "She's north-east". */
internal fun Context.directionText(state: WidgetState): String? = state.direction?.let {
    getString(R.string.home_star_direction, getString(it.labelRes()).lowercase(locale()))
}

internal fun Context.daysUntilText(reunion: WidgetReunion): String = if (reunion.isToday) {
    getString(R.string.widget_reunion_today)
} else {
    resources.getQuantityString(R.plurals.widget_days_until, reunion.daysUntil.toInt(), reunion.daysUntil)
}

internal fun Context.daysLabel(days: Long): String = resources.getQuantityString(R.plurals.widget_days_label, days.toInt())

internal fun Context.reunionDate(date: LocalDate): String {
    val locale = locale()
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, DatePattern), locale))
}

internal fun Context.newMemoriesText(count: Int): String =
    resources.getQuantityString(R.plurals.widget_new_memories, count, count)

private fun Context.locale(): Locale = resources.configuration.locales[0]

private const val DatePattern = "EEEdMMM"
