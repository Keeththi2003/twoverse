package app.twoverse.core.common

import app.twoverse.core.model.DistanceUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DistanceFormatTest {

    @Test
    fun underOneHundredShowsOneDecimal() {
        assertEquals("94.6", formatDistance(94.598, DistanceUnit.Kilometres, Locale.US))
    }

    @Test
    fun smallDistancesKeepOneDecimal() {
        assertEquals("0.4", formatDistance(0.42, DistanceUnit.Kilometres, Locale.US))
    }

    @Test
    fun oneHundredAndAboveShowsWholeNumbers() {
        assertEquals("150", formatDistance(150.4, DistanceUnit.Kilometres, Locale.US))
    }

    @Test
    fun valueThatRoundsUpToOneHundredShowsWholeNumber() {
        assertEquals("100", formatDistance(99.97, DistanceUnit.Kilometres, Locale.US))
    }

    @Test
    fun milesAreConvertedFromKilometres() {
        assertEquals("58.8", formatDistance(94.6, DistanceUnit.Miles, Locale.US))
    }

    @Test
    fun thousandsAreGroupedForTheLocale() {
        assertEquals("14,285", formatDistance(14_285.2, DistanceUnit.Kilometres, Locale.US))
        assertEquals("14.285", formatDistance(14_285.2, DistanceUnit.Kilometres, Locale.GERMANY))
        assertEquals("999", formatDistance(999.0, DistanceUnit.Kilometres, Locale.US))
        assertEquals("1,000", formatDistance(999.6, DistanceUnit.Kilometres, Locale.US))
    }

    @Test
    fun longDistancesInMilesAreGroupedToo() {
        assertEquals("8,876", formatDistance(14_285.0, DistanceUnit.Miles, Locale.US))
    }

    @Test
    fun decimalSeparatorFollowsLocale() {
        assertEquals("94,6", formatDistance(94.6, DistanceUnit.Kilometres, Locale.GERMANY))
    }
}
