package app.twoverse.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class MemoryExpiryTest {

    private val now = Instant.parse("2026-09-26T12:00:00Z")

    private fun badgeIn(left: Duration) = expiryBadge(now + left, now)

    @Test
    fun memoryWithoutExpiryHasNoBadge() {
        assertNull(expiryBadge(null, now))
        assertFalse(isExpired(null, now))
    }

    @Test
    fun expiredMemoryHasNoBadge() {
        assertNull(badgeIn(Duration.ofMinutes(-1)))
        assertNull(badgeIn(Duration.ZERO))
        assertTrue(isExpired(now, now))
    }

    @Test
    fun upToOneDayShowsHoursRoundedUp() {
        assertEquals(ExpiryBadge.Hours(24), badgeIn(Duration.ofHours(24)))
        assertEquals(ExpiryBadge.Hours(24), badgeIn(Duration.ofHours(23).plusMinutes(1)))
        assertEquals(ExpiryBadge.Hours(1), badgeIn(Duration.ofMinutes(5)))
    }

    @Test
    fun moreThanOneDayShowsNearestDay() {
        assertEquals(ExpiryBadge.Days(1), badgeIn(Duration.ofHours(25)))
        assertEquals(ExpiryBadge.Days(2), badgeIn(Duration.ofDays(2)))
        assertEquals(ExpiryBadge.Days(7), badgeIn(Duration.ofDays(7).minusMinutes(1)))
        assertEquals(ExpiryBadge.Days(30), badgeIn(Duration.ofDays(30)))
    }
}
