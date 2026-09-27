package app.twoverse.core.data.supabase

import app.twoverse.core.model.CoupleCode
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.LocationSharing
import app.twoverse.core.model.UserLocation
import app.twoverse.core.model.UserProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime

/** Rows and RPC results as Supabase returns them; mapped to core/model before leaving the data layer. */
@Serializable
internal data class ProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
) {
    fun toModel() = UserProfile(id = id, displayName = displayName)
}

@Serializable
internal data class CoupleDto(
    val id: String,
    @SerialName("user_a") val userA: String,
    @SerialName("user_b") val userB: String? = null,
    val status: String,
    @SerialName("connected_at") val connectedAt: String? = null,
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
