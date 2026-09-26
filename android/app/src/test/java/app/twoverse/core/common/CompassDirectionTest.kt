package app.twoverse.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class CompassDirectionTest {

    @Test
    fun bearingsMapToNearestCompassPoint() {
        assertEquals(CompassDirection.North, CompassDirection.fromBearing(0.0))
        assertEquals(CompassDirection.North, CompassDirection.fromBearing(22.4))
        assertEquals(CompassDirection.NorthEast, CompassDirection.fromBearing(42.0))
        assertEquals(CompassDirection.East, CompassDirection.fromBearing(90.0))
        assertEquals(CompassDirection.SouthWest, CompassDirection.fromBearing(225.0))
        assertEquals(CompassDirection.NorthWest, CompassDirection.fromBearing(315.0))
        assertEquals(CompassDirection.North, CompassDirection.fromBearing(350.0))
    }

    @Test
    fun negativeAndLargeBearingsAreNormalised() {
        assertEquals(CompassDirection.West, CompassDirection.fromBearing(-90.0))
        assertEquals(CompassDirection.NorthEast, CompassDirection.fromBearing(405.0))
    }
}
