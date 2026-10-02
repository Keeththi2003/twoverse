package app.twoverse.core.data.supabase

import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.CoupleCode
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.LocationSharing
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender
import app.twoverse.core.model.ProfileSettings
import app.twoverse.core.model.Reunion
import app.twoverse.core.model.ShootingStar
import app.twoverse.core.model.StarContent
import app.twoverse.core.model.StarLayout
import app.twoverse.core.model.StarPhotoFit
import app.twoverse.core.model.UserLocation
import app.twoverse.core.model.UserProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale

/** Rows and RPC results as Supabase returns them; mapped to core/model before leaving the data layer. */
@Serializable
internal data class ProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("time_zone") val timeZone: String? = null,
) {
    fun toModel() = UserProfile(id = id, displayName = displayName, timeZone = timeZone)
}

@Serializable
internal data class ProfileSettingsDto(
    @SerialName("lock_ours") val lockOurs: Boolean,
    @SerialName("distance_unit") val distanceUnit: String,
    val appearance: String,
) {
    fun toModel() = ProfileSettings(
        lockOurs = lockOurs,
        distanceUnit = if (distanceUnit == UnitMiles) DistanceUnit.Miles else DistanceUnit.Kilometres,
        appearance = AppearanceMode.entries.firstOrNull { it.toColumn() == appearance } ?: AppearanceMode.System,
    )
}

private const val UnitKilometres = "km"
private const val UnitMiles = "mi"

internal fun DistanceUnit.toColumn(): String = when (this) {
    DistanceUnit.Kilometres -> UnitKilometres
    DistanceUnit.Miles -> UnitMiles
}

/** The profiles.appearance values: 'system', 'light', 'dark'. */
internal fun AppearanceMode.toColumn(): String = name.lowercase(Locale.ROOT)

@Serializable
internal data class CoupleDto(
    val id: String,
    @SerialName("user_a") val userA: String,
    @SerialName("user_b") val userB: String? = null,
    val status: String,
    @SerialName("connected_at") val connectedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("purge_after") val purgeAfter: String? = null,
    @SerialName("reconnect_requested_by") val reconnectRequestedBy: String? = null,
)

@Serializable
internal data class CoupleCodeDto(
    val code: String,
    @SerialName("expires_at") val expiresAt: String,
) {
    fun toModel() = CoupleCode(code = code, expiresAt = parseTimestamp(expiresAt))
}

@Serializable
internal data class LocationDto(
    @SerialName("user_id") val userId: String,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("accuracy_m") val accuracyMeters: Float? = null,
    val precision: String = PrecisionApproximate,
    @SerialName("sharing_enabled") val sharingEnabled: Boolean = false,
    @SerialName("updated_at") val updatedAt: String,
) {
    fun toSharing() = LocationSharing(enabled = sharingEnabled, precision = precision.toPrecision())

    /** Null while there is no position (sharing off, or nothing uploaded yet). */
    fun toModel(): UserLocation? {
        if (lat == null || lng == null) return null
        return UserLocation(
            userId = userId,
            latitude = lat,
            longitude = lng,
            accuracyMeters = accuracyMeters ?: 0f,
            precision = precision.toPrecision(),
            city = null,
            updatedAt = parseTimestamp(updatedAt),
        )
    }
}

@Serializable
internal data class ReunionDto(
    @SerialName("couple_id") val coupleId: String,
    @SerialName("meet_at") val meetAt: String,
    @SerialName("has_time") val hasTime: Boolean,
    val place: String? = null,
    val note: String? = null,
    @SerialName("date_set_at") val dateSetAt: String,
) {
    fun toModel() = Reunion(
        meetAt = parseTimestamp(meetAt),
        hasTime = hasTime,
        place = place,
        note = note,
        dateSetAt = parseTimestamp(dateSetAt),
    )
}

@Serializable
internal data class MemoryDto(
    val id: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("storage_path") val storagePath: String,
    val caption: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("allow_keep") val allowKeep: Boolean,
    @SerialName("viewed_at") val viewedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
) {
    fun toModel(myUserId: String, imageUrl: String?) = Memory(
        id = id,
        sender = if (senderId == myUserId) MemorySender.Me else MemorySender.Partner,
        imageUrl = imageUrl,
        caption = caption,
        createdAt = parseTimestamp(createdAt),
        expiresAt = expiresAt?.let(::parseTimestamp),
        allowKeep = allowKeep,
        viewedAt = viewedAt?.let(::parseTimestamp),
    )
}

@Serializable
internal data class ShootingStarDto(
    val id: String,
    val layout: String,
    val eyebrow: String? = null,
    val title: String? = null,
    val message: String? = null,
    val signature: String? = null,
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("photo_fit") val photoFit: String,
    @SerialName("show_at") val showAt: String? = null,
    @SerialName("seen_at") val seenAt: String? = null,
    @SerialName("created_at") val createdAt: String,
) {
    fun toModel(photoUrl: String? = null) = ShootingStar(
        id = id,
        content = StarContent(
            layout = StarLayout.entries.firstOrNull { it.toColumn() == layout } ?: StarLayout.PhotoMessage,
            eyebrow = eyebrow,
            title = title,
            message = message,
            signature = signature,
            photoFit = StarPhotoFit.entries.firstOrNull { it.toColumn() == photoFit } ?: StarPhotoFit.Fill,
        ),
        hasPhoto = photoPath != null,
        photoUrl = photoUrl,
        showAt = showAt?.let(::parseTimestamp),
        seenAt = seenAt?.let(::parseTimestamp),
        createdAt = parseTimestamp(createdAt),
    )
}

/** The shooting_stars.layout values: 'photo_message', 'message_only', 'full_photo'. */
internal fun StarLayout.toColumn(): String = when (this) {
    StarLayout.PhotoMessage -> "photo_message"
    StarLayout.MessageOnly -> "message_only"
    StarLayout.FullPhoto -> "full_photo"
}

/** The shooting_stars.photo_fit values: 'fill', 'fit'. */
internal fun StarPhotoFit.toColumn(): String = name.lowercase(Locale.ROOT)

internal const val PrecisionApproximate = "approximate"
internal const val PrecisionPrecise = "precise"

internal fun String.toPrecision(): LocationPrecision =
    if (this == PrecisionPrecise) LocationPrecision.Precise else LocationPrecision.Approximate

internal fun LocationPrecision.toColumn(): String = when (this) {
    LocationPrecision.Approximate -> PrecisionApproximate
    LocationPrecision.Precise -> PrecisionPrecise
}

/** Postgres timestamptz ("2026-09-27T09:17:02.123+00:00") to an Instant (UTC). */
internal fun parseTimestamp(value: String): Instant = OffsetDateTime.parse(value).toInstant()
