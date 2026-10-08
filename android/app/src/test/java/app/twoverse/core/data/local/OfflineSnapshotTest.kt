package app.twoverse.core.data.local

import androidx.datastore.core.CorruptionException
import app.twoverse.core.model.Couple
import app.twoverse.core.model.CoupleStatus
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.LocationSharing
import app.twoverse.core.model.Meetup
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender
import app.twoverse.core.model.Reunion
import app.twoverse.core.model.UserLocation
import app.twoverse.core.model.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate

class OfflineSnapshotTest {

    private val now = Instant.parse("2026-09-28T09:00:00Z")

    private fun location(userId: String) = UserLocation(
        userId = userId,
        latitude = 6.93,
        longitude = 79.86,
        accuracyMeters = 25f,
        precision = LocationPrecision.Precise,
        city = null,
        updatedAt = now,
    )

    private val full = OfflineSnapshot(
        ownerId = "me",
        couple = Couple(
            "couple",
            UserProfile("her", "Ammu Perera", timeZone = "Europe/London"),
            CoupleStatus.Active,
            connectedAt = now,
            togetherSince = LocalDate.of(2024, 2, 29),
        ),
        sharing = LocationSharing(enabled = true, precision = LocationPrecision.Precise),
        myLocation = location("me"),
        partnerLocation = location("her"),
        reunion = Reunion(meetAt = now.plusSeconds(86_400), hasTime = true, place = "Kandy", note = null, dateSetAt = now),
        memories = listOf(
            Memory(
                id = "memory",
                sender = MemorySender.Partner,
                imageUrl = null,
                caption = "Sunset",
                createdAt = now,
                expiresAt = now.plusSeconds(3_600),
                allowKeep = true,
                viewedAt = null,
            ),
        ),
        hasWaitingStar = true,
        meetups = listOf(
            Meetup("meetup", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 14), "Kandy", "Long weekend", fromReunionAt = now),
            Meetup("single", LocalDate.of(2026, 9, 20), null, null, null),
        ),
    )

    @Test
    fun everySavedFieldSurvivesARestart() = runTest {
        val out = ByteArrayOutputStream()
        OfflineSnapshotSerializer.writeTo(full.toCached(), out)

        val read = OfflineSnapshotSerializer.readFrom(ByteArrayInputStream(out.toByteArray()))

        assertEquals(full, read?.toModel())
    }

    @Test
    fun anEmptyFileMeansNothingIsSaved() = runTest {
        assertNull(OfflineSnapshotSerializer.readFrom(ByteArrayInputStream(ByteArray(0))))
    }

    @Test(expected = CorruptionException::class)
    fun anUnreadableFileIsReportedAsCorrupt() = runTest {
        OfflineSnapshotSerializer.readFrom(ByteArrayInputStream("{not json".encodeToByteArray()))
    }

    @Test
    fun anotherUsersDataIsNeverReused() {
        assertEquals(OfflineSnapshot("someone-else"), full.forOwner("someone-else"))
        assertEquals(full, full.forOwner("me"))
        assertEquals(OfflineSnapshot("me"), null.forOwner("me"))
    }

    @Test
    fun endingTheCoupleKeepsOnlyTheUsersOwnData() {
        assertEquals(
            OfflineSnapshot(ownerId = "me", sharing = full.sharing, myLocation = full.myLocation),
            full.withoutCouple(),
        )
    }
}
