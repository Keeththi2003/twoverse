package app.twoverse.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class CountdownTest {

    private val now = Instant.parse("2026-09-26T01:35:50Z")

    @Test
    fun splitsTimeLeftIntoDaysHoursMinutesSeconds() {
        val target = now + Duration.ofDays(12).plusHours(8).plusMinutes(24).plusSeconds(10)

        assertEquals(CountdownTime(days = 12, hours = 8, minutes = 24, seconds = 10), countdownUntil(target, now))
    }

    @Test
    fun partialSecondsRoundDown() {
        val target = now + Duration.ofMillis(1_999)

        assertEquals(CountdownTime(days = 0, hours = 0, minutes = 0, seconds = 1), countdownUntil(target, now))
    }

    @Test
    fun passedTargetIsZeroAndReached() {
        val countdown = countdownUntil(now - Duration.ofHours(1), now)

        assertEquals(CountdownTime(days = 0, hours = 0, minutes = 0, seconds = 0), countdown)
        assertTrue(countdown.isReached)
    }

    @Test
    fun oneSecondLeftIsNotReached() {
        assertFalse(countdownUntil(now + Duration.ofSeconds(1), now).isReached)
    }

    @Test
    fun progressIsShareOfWaitElapsed() {
        val start = now - Duration.ofDays(11)
        val target = now + Duration.ofDays(9)

        assertEquals(0.55f, countdownProgress(start, target, now), 0.001f)
    }

    @Test
    fun progressIsClampedToZeroAndOne() {
        assertEquals(0f, countdownProgress(now + Duration.ofDays(1), now + Duration.ofDays(2), now), 0f)
        assertEquals(1f, countdownProgress(now - Duration.ofDays(2), now - Duration.ofDays(1), now), 0f)
    }
}
