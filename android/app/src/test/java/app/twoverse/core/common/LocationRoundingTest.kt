package app.twoverse.core.common

import app.twoverse.core.model.DevicePosition
import app.twoverse.core.model.LocationPrecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LocationRoundingTest {

    private val position = DevicePosition(latitude = 6.927123, longitude = 79.861244, accuracyMeters = 12f)

    @Test
    fun approximateRoundsToTwoDecimals() {
        val rounded = position.forPrecision(LocationPrecision.Approximate)

        assertEquals(6.93, rounded.latitude, 0.0)
        assertEquals(79.86, rounded.longitude, 0.0)
    }

    @Test
    fun approximateMovesAtMostAboutOneKilometre() {
        val rounded = position.forPrecision(LocationPrecision.Approximate)
        val shift = distanceKm(location(position.latitude, position.longitude), location(rounded.latitude, rounded.longitude))

        assertEquals(true, shift < 1.0)
    }

    @Test
    fun approximateReportsAtLeastOneKilometreAccuracy() {
        assertEquals(1_000f, position.forPrecision(LocationPrecision.Approximate).accuracyMeters)
        assertEquals(2_500f, position.copy(accuracyMeters = 2_500f).forPrecision(LocationPrecision.Approximate).accuracyMeters)
    }

    @Test
    fun negativeCoordinatesRoundTheSameWay() {
        val rounded = DevicePosition(-33.868820, -151.209295, 5f).forPrecision(LocationPrecision.Approximate)

        assertEquals(-33.87, rounded.latitude, 0.0)
        assertEquals(-151.21, rounded.longitude, 0.0)
    }

    @Test
    fun preciseIsUnchanged() {
        assertSame(position, position.forPrecision(LocationPrecision.Precise))
    }
}
