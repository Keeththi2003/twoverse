package app.twoverse.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassMathTest {

    @Test
    fun anglesAreNormalisedToOneTurn() {
        assertEquals(10.0, normalizeDegrees(370.0), 1e-9)
        assertEquals(350.0, normalizeDegrees(-10.0), 1e-9)
        assertEquals(0.0, normalizeDegrees(720.0), 1e-9)
    }

    @Test
    fun shortestTurnCrossesNorthTheShortWay() {
        assertEquals(20.0, shortestTurn(350.0, 10.0), 1e-9)
        assertEquals(-20.0, shortestTurn(10.0, 350.0), 1e-9)
        assertEquals(180.0, shortestTurn(0.0, 180.0), 1e-9)
        assertEquals(-90.0, shortestTurn(90.0, 0.0), 1e-9)
    }

    @Test
    fun trueHeadingAddsTheDeclination() {
        assertEquals(42.0, trueHeading(40.0, 2.0), 1e-9)
        assertEquals(358.0, trueHeading(3.0, -5.0), 1e-9)
    }

    @Test
    fun pointingAtThePartnerAllowsFiveDegrees() {
        assertTrue(isPointingAt(bearingDegrees = 42.0, headingDegrees = 37.0))
        assertTrue(isPointingAt(bearingDegrees = 2.0, headingDegrees = 358.0))
        assertFalse(isPointingAt(bearingDegrees = 42.0, headingDegrees = 36.9))
        assertTrue(isPointingAt(bearingDegrees = 42.0, headingDegrees = 402.0))
    }

    @Test
    fun smootherStartsAtTheFirstReading() {
        assertEquals(90.0, AngleSmoother(0.2).smooth(90.0), 1e-9)
    }

    @Test
    fun smootherMovesPartWayTowardsEachReading() {
        val smoother = AngleSmoother(0.25)
        smoother.smooth(0.0)

        assertEquals(10.0, smoother.smooth(40.0), 1e-9)
        assertEquals(17.5, smoother.smooth(40.0), 1e-9)
    }

    @Test
    fun smootherCrossesNorthWithoutSpinningAndStaysContinuous() {
        val smoother = AngleSmoother(0.5)
        smoother.smooth(350.0)

        val next = smoother.smooth(10.0)

        assertEquals(360.0, next, 1e-9)
        assertEquals(0.0, normalizeDegrees(next), 1e-9)
    }

    @Test
    fun smootherSettlesOnASteadyReading() {
        val smoother = AngleSmoother(0.15)
        smoother.smooth(0.0)
        var last = 0.0
        repeat(100) { last = smoother.smooth(120.0) }

        assertEquals(120.0, last, 0.01)
    }
}
