package app.twoverse.widget

import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.data.local.OfflineSnapshot
import app.twoverse.core.model.Couple
import app.twoverse.core.model.CoupleStatus
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.LocationSharing
import app.twoverse.core.model.Reunion
import app.twoverse.core.model.UserLocation
import app.twoverse.core.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.Locale

class WidgetStateTest {

    private val now = Instant.parse("2026-09-28T09:00:00Z")

    private fun location(userId: String, latitude: Double, age: Duration) = UserLocation(
        userId = userId,
        latitude = latitude,
        longitude = 79.86,
        accuracyMeters = 20f,
        precision = LocationPrecision.Precise,
        city = null,
        updatedAt = now - age,
    )

    private fun snapshot(partnerAge: Duration = Duration.ofSeconds(30), sharing: Boolean = true) = OfflineSnapshot(
        ownerId = "me",
        couple = Couple("couple", UserProfile("her", "Her"), CoupleStatus.Active, connectedAt = null),
        sharing = LocationSharing(enabled = sharing),
        myLocation = location("me", latitude = 6.93, age = Duration.ofMinutes(1)),
        partnerLocation = location("her", latitude = 7.29, age = partnerAge),
        reunion = Reunion(meetAt = now + Duration.ofDays(12), hasTime = false, place = null, note = null, dateSetAt = now),
    )

    private fun state(snapshot: OfflineSnapshot?, unit: DistanceUnit = DistanceUnit.Kilometres) =
        widgetState(snapshot, unit, now, Locale.US)

    @Test
    fun showsDistanceFreshnessAndDaysUntilTheReunion() {
        val state = state(snapshot())

        assertEquals("40.0", state.distance)
        assertEquals(LocationFreshness.Live, state.freshness)
        assertEquals(12L, state.daysUntilReunion)
    }

    @Test
    fun savedPositionsAgeSoTheyAreNeverShownAsLive() {
        assertEquals(LocationFreshness.Recent, state(snapshot(partnerAge = Duration.ofMinutes(10))).freshness)
        assertEquals(LocationFreshness.Outdated, state(snapshot(partnerAge = Duration.ofHours(3))).freshness)
    }

    @Test
    fun usesTheChosenUnit() {
        assertEquals(DistanceUnit.Miles, state(snapshot(), DistanceUnit.Miles).distanceUnit)
        assertEquals("24.9", state(snapshot(), DistanceUnit.Miles).distance)
    }

    @Test
    fun noDistanceWhileSharingIsOff() {
        val state = state(snapshot(sharing = false))

        assertNull(state.distance)
        assertEquals(LocationFreshness.Unavailable, state.freshness)
    }

    @Test
    fun withoutACoupleNothingSharedIsShown() {
        val state = state(snapshot().copy(couple = null))

        assertNull(state.distance)
        assertNull(state.daysUntilReunion)
    }

    @Test
    fun nothingSavedYet() {
        val state = state(null)

        assertNull(state.distance)
        assertEquals(LocationFreshness.Unavailable, state.freshness)
        assertNull(state.daysUntilReunion)
    }
}
