package app.twoverse.core.data.push

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class PingThrottleTest {

    private var now = Instant.parse("2026-09-27T12:00:00Z")
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
    }
    private val throttle = PingThrottle(clock, Duration.ofMinutes(1))

    @Test
    fun firstPingIsAllowed() {
        assertTrue(throttle.tryAcquire())
    }

    @Test
    fun pingsWithinAMinuteAreSkipped() {
        throttle.tryAcquire()
        now = now.plusSeconds(59)

        assertFalse(throttle.tryAcquire())
    }

    @Test
    fun pingIsAllowedAgainAfterAMinute() {
        throttle.tryAcquire()
        now = now.plusSeconds(60)

        assertTrue(throttle.tryAcquire())
    }

    @Test
    fun skippedPingsDoNotExtendTheWait() {
        throttle.tryAcquire()
        now = now.plusSeconds(30)
        throttle.tryAcquire()
        now = now.plusSeconds(30)

        assertTrue(throttle.tryAcquire())
    }
}
