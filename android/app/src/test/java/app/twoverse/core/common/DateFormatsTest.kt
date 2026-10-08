package app.twoverse.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class DateFormatsTest {

    private val date = LocalDate.of(2025, 6, 22)

    private fun long(locale: Locale): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(dateLocale(locale)))

    @Test
    fun sriLankanEnglishWritesTheDayFirst() {
        assertEquals("22 June 2025", long(Locale.forLanguageTag("en-LK")))
    }

    @Test
    fun britishAndIndianEnglishStayDayFirst() {
        assertEquals("22 June 2025", long(Locale.UK))
        assertEquals("22 June 2025", long(Locale.forLanguageTag("en-IN")))
    }

    @Test
    fun usAndCanadianEnglishKeepTheMonthFirst() {
        assertEquals(Locale.US, dateLocale(Locale.US))
        assertEquals("June 22, 2025", long(Locale.US))
        assertEquals(Locale.CANADA, dateLocale(Locale.CANADA))
        assertEquals(Locale.forLanguageTag("en-PR"), dateLocale(Locale.forLanguageTag("en-PR")))
    }

    @Test
    fun otherLanguagesKeepTheirOwnLocale() {
        assertEquals(Locale.GERMANY, dateLocale(Locale.GERMANY))
        assertEquals(Locale.forLanguageTag("si-LK"), dateLocale(Locale.forLanguageTag("si-LK")))
        assertEquals(Locale.forLanguageTag("ta-LK"), dateLocale(Locale.forLanguageTag("ta-LK")))
    }

    @Test
    fun englishWithoutARegionIsLeftAlone() {
        assertEquals(Locale.ENGLISH, dateLocale(Locale.ENGLISH))
    }
}
