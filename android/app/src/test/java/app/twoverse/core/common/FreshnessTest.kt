package app.twoverse.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FreshnessTest {

    private val now = Instant.parse("2026-09-26T12:00:00Z")

    private fun freshnessAfter(age: Duration) = locationFreshness(now - age, now)

    @Test
    fun missingUpdateIsUnavailable() {
        assertEquals(LocationFreshness.Unavailable, locationFreshness(null, now))
    }

    @Test
    fun upToTwoMinutesIsLive() {
        assertEquals(LocationFreshness.Live, freshnessAfter(Duration.ofSeconds(12)))
        assertEquals(LocationFreshness.Live, freshnessAfter(Duration.ofMinutes(2)))
    }

    @Test
    fun upToThirtyMinutesIsRecent() {
        assertEquals(LocationFreshness.Recent, freshnessAfter(Duration.ofMinutes(2).plusSeconds(1)))
        assertEquals(LocationFreshness.Recent, freshnessAfter(Duration.ofMinutes(30)))
    }

    @Test
    fun overThirtyMinutesIsOutdated() {
        assertEquals(LocationFreshness.Outdated, freshnessAfter(Duration.ofMinutes(30).plusSeconds(1)))
    }

    @Test
    fun thresholdsAreTheSrsValues() {
        assertEquals(Duration.ofMinutes(2), FreshnessThresholds.Live)
        assertEquals(Duration.ofMinutes(30), FreshnessThresholds.Recent)
    }

    @Test
    fun futureTimestampCountsAsLive() {
        assertEquals(LocationFreshness.Live, locationFreshness(now + Duration.ofSeconds(5), now))
    }

    @Test
    fun elapsedTimeUsesLargestWholeUnit() {
        assertEquals(ElapsedTime.Seconds(12), elapsedTime(now - Duration.ofSeconds(12), now))
        assertEquals(ElapsedTime.Minutes(5), elapsedTime(now - Duration.ofSeconds(5 * 60 + 59), now))
        assertEquals(ElapsedTime.Hours(3), elapsedTime(now - Duration.ofMinutes(3 * 60 + 10), now))
        assertEquals(ElapsedTime.Days(2), elapsedTime(now - Duration.ofHours(50), now))
    }
}
