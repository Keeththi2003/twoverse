package app.twoverse.core.common

import android.text.format.DateFormat
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale

/** English is written month first ("June 22, 2025") only in the US, its territories and Canada. */
private val MonthFirstEnglishRegions = setOf("US", "AS", "GU", "MP", "PR", "UM", "VI", "CA")

/** International English: day first, "22 June 2025". */
private val InternationalEnglish: Locale = Locale.forLanguageTag("en-001")

/**
 * The locale to format dates in, from the device locale. Locale data has no English date formats
 * for many regions (Sri Lanka, Bangladesh, Nepal…), which then fall back to US English and show
 * "June 22, 2025". English outside the month-first regions uses international English instead,
 * which is what those regions write; every other language keeps its own locale.
 */
fun dateLocale(locale: Locale): Locale =
    if (locale.language == "en" && locale.country.isNotEmpty() && locale.country !in MonthFirstEnglishRegions) {
        InternationalEnglish
    } else {
        locale
    }

/** Formats [date] from a skeleton such as "dMMMMy" in the order and words of the device locale. */
fun formatDate(date: TemporalAccessor, skeleton: String, locale: Locale): String {
    val formatLocale = dateLocale(locale)
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(formatLocale, skeleton), formatLocale).format(date)
}
