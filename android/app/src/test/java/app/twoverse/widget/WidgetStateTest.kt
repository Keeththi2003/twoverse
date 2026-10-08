package app.twoverse.widget

import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.data.local.OfflineSnapshot
import app.twoverse.core.model.Couple
import app.twoverse.core.model.CoupleStatus
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.LocationSharing
import app.twoverse.core.model.Meetup
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender
import app.twoverse.core.model.Reunion
import app.twoverse.core.model.UserLocation
import app.twoverse.core.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.Locale

class WidgetStateTest {

    private val now = Instant.parse("2026-09-28T09:00:00Z")
    private val colombo = ZoneId.of("Asia/Colombo")

    private fun location(userId: String, latitude: Double, age: Duration) = UserLocation(
        userId = userId,
        latitude = latitude,
        longitude = 79.86,
        accuracyMeters = 20f,
        precision = LocationPrecision.Precise,
        city = null,
        updatedAt = now - age,
    )

    private fun memory(id: String, sender: MemorySender, viewed: Boolean) = Memory(
        id = id,
        sender = sender,
        imageUrl = null,
        caption = "A caption the widget never shows",
        createdAt = now,
        expiresAt = null,
        allowKeep = false,
        viewedAt = if (viewed) now else null,
    )

    private fun snapshot(
        partnerAge: Duration = Duration.ofSeconds(30),
        sharing: Boolean = true,
        partnerTimeZone: String? = null,
        reunion: Reunion? = Reunion(
            meetAt = now + Duration.ofDays(16),
            hasTime = false,
            place = null,
            note = null,
            dateSetAt = now - Duration.ofDays(16),
        ),
    ) = OfflineSnapshot(
        ownerId = "me",
        couple = Couple("couple", UserProfile("her", "Ammu Perera", timeZone = partnerTimeZone), CoupleStatus.Active, connectedAt = null),
        sharing = LocationSharing(enabled = sharing),
        myLocation = location("me", latitude = 6.93, age = Duration.ofMinutes(1)),
        // North of the user.
        partnerLocation = location("her", latitude = 7.29, age = partnerAge),
        reunion = reunion,
    )

    private fun state(
        snapshot: OfflineSnapshot?,
        unit: DistanceUnit = DistanceUnit.Kilometres,
        online: Boolean = true,
        at: Instant = now,
    ) = widgetState(snapshot, unit, online, at, colombo, Locale.US)

    @Test
    fun showsDistanceFreshnessDirectionAndTheReunion() {
        val state = state(snapshot())

        assertEquals(WidgetLocation.Available, state.location)
        assertEquals("40.0", state.distance)
        assertEquals(LocationFreshness.Live, state.freshness)
        assertEquals(CompassDirection.North, state.direction)
        assertEquals(16L, state.reunion?.daysUntil)
        assertEquals(LocalDate.of(2026, 10, 14), state.reunion?.date)
        assertEquals(0.5f, state.reunion?.progress)
        assertFalse(state.isOffline)
    }

    @Test
    fun savedPositionsAgeSoTheyAreNeverShownAsLive() {
        val recent = state(snapshot(partnerAge = Duration.ofMinutes(10)))
        val outdated = state(snapshot(partnerAge = Duration.ofHours(3)))

        assertEquals(LocationFreshness.Recent, recent.freshness)
        assertEquals(ElapsedTime.Minutes(10), recent.updatedAgo)
        assertEquals(LocationFreshness.Outdated, outdated.freshness)
        assertEquals(ElapsedTime.Hours(3), outdated.updatedAgo)
    }

    @Test
    fun offlineStillShowsSavedDataAgeingNormally() {
        val state = state(snapshot(partnerAge = Duration.ofMinutes(10)), online = false)

        assertTrue(state.isOffline)
        assertEquals("40.0", state.distance)
        assertEquals(LocationFreshness.Recent, state.freshness)
    }

    @Test
    fun longDistancesUseTheLocaleThousandsSeparator() {
        val farAway = snapshot().copy(partnerLocation = location("her", latitude = -60.0, age = Duration.ofSeconds(30)))

        assertEquals("7,442", state(farAway).distance)
    }

    @Test
    fun usesTheChosenUnit() {
        val state = state(snapshot(), DistanceUnit.Miles)

        assertEquals(DistanceUnit.Miles, state.distanceUnit)
        assertEquals("24.9", state.distance)
    }

    @Test
    fun sharingOffHasItsOwnStateAndNoPosition() {
        val state = state(snapshot(sharing = false))

        assertEquals(WidgetLocation.SharingOff, state.location)
        assertNull(state.distance)
        assertNull(state.direction)
        assertNull(state.updatedAgo)
        assertEquals(LocationFreshness.Unavailable, state.freshness)
        assertEquals(16L, state.reunion?.daysUntil)
    }

    @Test
    fun aMissingPartnerLocationMeansDistanceUnavailable() {
        val state = state(snapshot().copy(partnerLocation = null))

        assertEquals(WidgetLocation.Unavailable, state.location)
        assertNull(state.distance)
    }

    @Test
    fun withoutACoupleNothingSharedIsShown() {
        val state = state(snapshot().copy(couple = null, memories = listOf(memory("m", MemorySender.Partner, false)), hasWaitingStar = true))

        assertEquals(WidgetLocation.NotPaired, state.location)
        assertFalse(state.isPaired)
        assertNull(state.distance)
        assertNull(state.reunion)
        assertEquals(0, state.newMemoryCount)
        assertFalse(state.hasWaitingStar)
    }

    @Test
    fun nothingSavedYetIsNotPaired() {
        val state = state(null)

        assertEquals(WidgetLocation.NotPaired, state.location)
        assertEquals(LocationFreshness.Unavailable, state.freshness)
        assertNull(state.reunion)
    }

    @Test
    fun noReunionDateAndAPastReunionShowNoCountdown() {
        assertNull(state(snapshot(reunion = null)).reunion)

        val past = snapshot().reunion?.copy(meetAt = now - Duration.ofDays(2))
        assertNull(state(snapshot(reunion = past)).reunion)
    }

    @Test
    fun theReunionDayIsToday() {
        val earlierToday = snapshot().reunion?.copy(meetAt = now - Duration.ofHours(1))

        val reunion = state(snapshot(reunion = earlierToday)).reunion

        assertTrue(reunion?.isToday == true)
        assertEquals(1f, reunion?.progress)
    }

    @Test
    fun herTimeZoneIsShownOnlyWhenItsClockDiffers() {
        assertEquals(ZoneId.of("Europe/London"), state(snapshot(partnerTimeZone = "Europe/London")).partnerTimeZone)
        assertNull(state(snapshot(partnerTimeZone = "Asia/Kolkata")).partnerTimeZone)
        assertNull(state(snapshot(partnerTimeZone = null)).partnerTimeZone)
        assertNull(state(snapshot(partnerTimeZone = "Not/AZone")).partnerTimeZone)
    }

    @Test
    fun onlyUnopenedMemoriesFromHerCountAsNew() {
        val memories = listOf(
            memory("a", MemorySender.Partner, viewed = false),
            memory("b", MemorySender.Partner, viewed = false),
            memory("c", MemorySender.Partner, viewed = true),
            memory("d", MemorySender.Me, viewed = false),
        )

        assertEquals(2, state(snapshot().copy(memories = memories)).newMemoryCount)
    }

    @Test
    fun aWaitingShootingStarIsFlagged() {
        assertTrue(state(snapshot().copy(hasWaitingStar = true)).hasWaitingStar)
        assertFalse(state(snapshot().copy(hasWaitingStar = null)).hasWaitingStar)
    }

    // Our Orbit (FR-WGT-8). "now" is 28 September 2026 in Colombo.

    private fun withSince(since: String?) =
        snapshot().copy(couple = snapshot().couple?.copy(togetherSince = since?.let(LocalDate::parse)))

    @Test
    fun withoutTogetherSinceTheOrbitPartIsLeftOut() {
        assertNull(state(withSince(null)).orbit)
    }

    @Test
    fun showsDaysTogetherAndTimesMet() {
        val meetups = listOf(
            Meetup("a", LocalDate.of(2026, 9, 1), null, null, null),
            Meetup("b", LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 22), null, null),
        )

        val orbit = state(withSince("2024-06-23").copy(meetups = meetups)).orbit

        assertEquals(Period.of(2, 3, 5), orbit?.period)
        assertEquals(828L, orbit?.totalDays)
        assertEquals(2, orbit?.timesMet)
    }

    @Test
    fun theAnniversaryShowsOnlyWithinThirtyDays() {
        assertEquals(30L, state(withSince("2024-10-28")).orbit?.anniversary?.daysUntil)
        assertNull(state(withSince("2024-10-29")).orbit?.anniversary)
        assertEquals(0L, state(withSince("2024-09-28")).orbit?.anniversary?.daysUntil)
    }

    @Test
    fun aDateNotBegunHereIsLeftOut() {
        assertNull(state(withSince("2026-09-29")).orbit)
    }

    @Test
    fun withoutACoupleThereIsNoOrbit() {
        assertNull(state(withSince("2024-06-23").copy(couple = null)).orbit)
    }
}
