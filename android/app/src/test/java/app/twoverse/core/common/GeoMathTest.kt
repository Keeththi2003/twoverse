package app.twoverse.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {

    @Test
    fun distanceBetweenSampleLocationsIs94Point6Km() {
        assertEquals(94.6, distanceKm(Colombo, SamplePartner), 0.05)
    }

    @Test
    fun distanceToSelfIsZero() {
        assertEquals(0.0, distanceKm(Colombo, Colombo), 1e-9)
    }

    @Test
    fun distanceIsSymmetric() {
        assertEquals(distanceKm(Colombo, SamplePartner), distanceKm(SamplePartner, Colombo), 1e-9)
    }

    @Test
    fun bearingToSamplePartnerIs42Degrees() {
        assertEquals(42.0, initialBearingDegrees(Colombo, SamplePartner), 0.1)
    }

    @Test
    fun oneDegreeOfLongitudeAtTheEquatorIsAbout111Km() {
        assertEquals(111.19, distanceKm(location(0.0, 0.0), location(0.0, 1.0)), 0.01)
    }

    @Test
    fun oppositeSidesOfTheEarthAreHalfItsCircumferenceApart() {
        assertEquals(20_015.1, distanceKm(location(0.0, 0.0), location(0.0, 180.0)), 0.5)
    }

    @Test
    fun bearingDueWestIs270Degrees() {
        assertEquals(270.0, initialBearingDegrees(location(0.0, 10.0), location(0.0, 9.0)), 1e-6)
    }
}
