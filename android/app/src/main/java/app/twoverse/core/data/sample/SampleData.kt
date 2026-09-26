package app.twoverse.core.data.sample

import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.BirthdayWelcome
import app.twoverse.core.model.Couple
import app.twoverse.core.model.CoupleCode
import app.twoverse.core.model.CoupleStatus
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender
import app.twoverse.core.model.Reunion
import app.twoverse.core.model.UserLocation
import app.twoverse.core.model.UserProfile
import app.twoverse.core.model.UserSettings
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Placeholder data from the mockups, used by the fake repositories and by previews.
 * Times are relative to app start so the countdown and expiry badges always look like the mockups.
 */
object SampleData {
    private val now: Instant = Instant.now()

    const val SUNSET_MEMORY_ID = "memory-9"

    val me = UserProfile(id = "user-me", displayName = "You")
    val partner = UserProfile(id = "user-partner", displayName = "Her")

    val couple = Couple(
        id = "couple-1",
        partner = partner,
        status = CoupleStatus.Active,
        connectedAt = LocalDate.of(2026, 2, 14).atStartOfDay(ZoneId.of("Asia/Colombo")).toInstant(),
    )

    val coupleCode = CoupleCode(code = "AB72-KP91", expiresAt = now + Duration.ofHours(24))

    /** 94.6 km apart at a bearing of 42° (north-east), matching the Home and Compass mockups. */
    val myLocation = UserLocation(
        userId = me.id,
        latitude = 6.9271,
        longitude = 79.8612,
        accuracyMeters = 1_000f,
        precision = LocationPrecision.Approximate,
        city = "Colombo",
        updatedAt = now - Duration.ofSeconds(5),
    )

    val partnerLocation = UserLocation(
        userId = partner.id,
        latitude = 7.5590,
        longitude = 80.4354,
        accuracyMeters = 1_000f,
        precision = LocationPrecision.Approximate,
        city = "Kandy",
        updatedAt = now - Duration.ofSeconds(12),
    )

    /** 10:00 local time, 13 days from today (12-and-a-bit days to go), in Kandy; set 15 days ago. */
    val reunion = Reunion(
        meetAt = LocalDate.now(ZoneId.systemDefault()).plusDays(13)
            .atTime(10, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant(),
        hasTime = true,
        place = "Kandy",
        note = null,
        updatedAt = now - Duration.ofDays(15),
    )

    /** 17 memories, newest first: two unviewed, three with expiry badges (24h, 7d, 2d). */
    val memories: List<Memory> = (0 until 17).map { index ->
        val fromPartner = index % 2 == 0
        val id = "memory-${index + 1}"
        val isSunset = id == SUNSET_MEMORY_ID
        val createdAt = now - Duration.ofHours(2L + index * 18L)
        Memory(
            id = id,
            sender = if (fromPartner) MemorySender.Partner else MemorySender.Me,
            imageUrl = null,
            caption = if (isSunset) "Our first sunset call." else null,
            createdAt = createdAt,
            expiresAt = when (index) {
                1 -> now + Duration.ofHours(24)
                4 -> now + Duration.ofDays(7)
                8 -> now + Duration.ofDays(2)
                else -> null
            },
            allowKeep = isSunset,
            viewedAt = if (index == 0 || index == 2) null else createdAt + Duration.ofMinutes(30),
        )
    }

    val birthdayWelcome = BirthdayWelcome(
        message = "I built a little universe,\njust for the two of us.",
        fromName = me.displayName,
        photoUrl = null,
        seen = false,
    )

    val settings = UserSettings(
        shareLocation = true,
        locationPrecision = LocationPrecision.Approximate,
        lockOurs = true,
        distanceUnit = DistanceUnit.Kilometres,
        appearance = AppearanceMode.System,
    )
}
