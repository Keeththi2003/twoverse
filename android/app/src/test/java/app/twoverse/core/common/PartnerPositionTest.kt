package app.twoverse.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class PartnerPositionTest {

    private val now = Instant.parse("2026-09-26T12:00:00Z")
    private val partner = SamplePartner.copy(updatedAt = now - Duration.ofSeconds(12))

    @Test
    fun bothLocationsGiveDistanceBearingAndFreshness() {
        val position = partnerPosition(Colombo, partner, sharingEnabled = true, now = now)

        assertEquals(94.6, position.distanceKm ?: 0.0, 0.05)
        assertEquals(42.0, position.bearingDegrees ?: 0.0, 0.1)
        assertEquals(LocationFreshness.Live, position.freshness)
        assertEquals(ElapsedTime.Seconds(12), position.updatedAgo)
        assertNull(position.unavailableReason)
    }

    @Test
    fun sharingOffHidesDistance() {
        val position = partnerPosition(Colombo, partner, sharingEnabled = false, now = now)

        assertNull(position.distanceKm)
        assertEquals(LocationFreshness.Unavailable, position.freshness)
        assertEquals(LocationUnavailableReason.SharingOff, position.unavailableReason)
    }

    @Test
    fun missingPartnerLocationIsUnavailable() {
        val position = partnerPosition(Colombo, null, sharingEnabled = true, now = now)

        assertNull(position.bearingDegrees)
        assertEquals(LocationUnavailableReason.PartnerUnavailable, position.unavailableReason)
    }

    @Test
    fun oldPartnerLocationIsOutdatedButStillShown() {
        val old = partner.copy(updatedAt = now - Duration.ofHours(2))
        val position = partnerPosition(Colombo, old, sharingEnabled = true, now = now)

        assertEquals(LocationFreshness.Outdated, position.freshness)
        assertEquals(94.6, position.distanceKm ?: 0.0, 0.05)
    }
}
