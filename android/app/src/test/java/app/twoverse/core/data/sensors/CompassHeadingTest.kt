package app.twoverse.core.data.sensors

import app.twoverse.core.common.location
import app.twoverse.core.data.fake.FakeHeadingSource
import app.twoverse.core.model.HeadingReading
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock

class CompassHeadingTest {

    private val clock = Clock.systemUTC()

    @Test
    fun declinationTurnsMagneticIntoTrueNorth() = runTest {
        val source = FakeHeadingSource(declination = -2.5)
        source.readings.emit(HeadingReading(magneticDegrees = 44.5, isAccurate = true))

        val sample = CompassHeading(source, clock).headings(flowOf(location(6.9, 79.8))).first()

        assertEquals(42.0, sample.degrees, 1e-9)
        assertTrue(sample.isAccurate)
    }

    @Test
    fun withoutALocationMagneticNorthIsUsed() = runTest {
        val source = FakeHeadingSource(declination = 10.0)
        source.readings.emit(HeadingReading(magneticDegrees = 90.0, isAccurate = false))

        val sample = CompassHeading(source, clock).headings(flowOf(null)).first()

        assertEquals(90.0, sample.degrees, 1e-9)
        assertFalse(sample.isAccurate)
    }

    @Test
    fun readingsAreSmoothed() = runTest {
        val source = FakeHeadingSource()
        val collected = async { CompassHeading(source, clock).headings(flowOf(null)).take(2).toList() }
        testScheduler.runCurrent()
        source.readings.emit(HeadingReading(0.0, true))
        testScheduler.runCurrent()
        source.readings.emit(HeadingReading(100.0, true))

        val degrees = collected.await().map { it.degrees }
        assertEquals(0.0, degrees[0], 1e-9)
        assertEquals(100.0 * CompassHeading.SmoothingFactor, degrees[1], 1e-9)
    }

    @Test
    fun phonesWithoutASensorGetNoHeadings() = runTest {
        val heading = CompassHeading(FakeHeadingSource(isAvailable = false), clock)

        assertFalse(heading.isAvailable)
        assertEquals(emptyList<Any>(), heading.headings(flowOf(null)).take(1).toList())
    }
}
