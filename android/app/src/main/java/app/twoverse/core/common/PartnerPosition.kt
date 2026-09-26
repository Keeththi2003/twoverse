package app.twoverse.core.common

import app.twoverse.core.model.UserLocation
import java.time.Instant

/** Why the distance or direction can't be shown (SRS §7). */
enum class LocationUnavailableReason { SharingOff, PartnerUnavailable }

/** Where the partner is relative to the user, as shown on Home and Your Star. */
data class PartnerPosition(
    val distanceKm: Double?,
    val bearingDegrees: Double?,
    val freshness: LocationFreshness,
    val updatedAgo: ElapsedTime?,
    val unavailableReason: LocationUnavailableReason?,
)

/**
 * Combines both latest locations (FR-LOC-8, FR-LOC-10, FR-LOC-11, FR-CMP-1). Distance and bearing are
 * null when either location is missing or the user's own sharing is off. Outdated data is still returned,
 * marked [LocationFreshness.Outdated], so the UI never presents it as live (BR-8).
 */
fun partnerPosition(
    myLocation: UserLocation?,
    partnerLocation: UserLocation?,
    sharingEnabled: Boolean,
    now: Instant,
): PartnerPosition {
    val unavailableReason = when {
        !sharingEnabled -> LocationUnavailableReason.SharingOff
        myLocation == null || partnerLocation == null -> LocationUnavailableReason.PartnerUnavailable
        else -> null
    }
    if (unavailableReason != null || myLocation == null || partnerLocation == null) {
        return PartnerPosition(
            distanceKm = null,
            bearingDegrees = null,
            freshness = LocationFreshness.Unavailable,
            updatedAgo = null,
            unavailableReason = unavailableReason ?: LocationUnavailableReason.PartnerUnavailable,
        )
    }
    return PartnerPosition(
        distanceKm = distanceKm(myLocation, partnerLocation),
        bearingDegrees = initialBearingDegrees(myLocation, partnerLocation),
        freshness = locationFreshness(partnerLocation.updatedAt, now),
        updatedAgo = elapsedTime(partnerLocation.updatedAt, now),
        unavailableReason = null,
    )
}
