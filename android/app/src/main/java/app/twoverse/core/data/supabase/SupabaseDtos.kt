package app.twoverse.core.data.supabase

import app.twoverse.core.model.CoupleCode
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

/** Postgres timestamptz ("2026-09-27T09:17:02.123+00:00") to an Instant (UTC). */
internal fun parseTimestamp(value: String): Instant = OffsetDateTime.parse(value).toInstant()
