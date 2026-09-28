package app.twoverse.widget

import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.common.countdownUntil
import app.twoverse.core.common.formatDistance
import app.twoverse.core.common.partnerPosition
import app.twoverse.core.data.local.OfflineSnapshot
import app.twoverse.core.model.DistanceUnit
import java.time.Instant
import java.util.Locale

/** What the widget shows (FR-WGT-1). Never photos or captions (FR-WGT-3). */
data class WidgetState(
    /** Formatted distance number, or null when unavailable. */
    val distance: String?,
    val distanceUnit: DistanceUnit,
    val freshness: LocationFreshness,
    /** Whole days until the reunion, or null when no date is set. */
    val daysUntilReunion: Long?,
)

/**
 * The widget reads only the data saved on the device, so it works without a connection.
 * Freshness comes from when the partner's position was recorded, so saved data turns
 * "Recent" and then "Outdated" and is never shown as live (BR-8).
 */
internal fun widgetState(
    snapshot: OfflineSnapshot?,
    distanceUnit: DistanceUnit,
    now: Instant,
    locale: Locale = Locale.getDefault(),
): WidgetState {
    val paired = snapshot?.couple != null
    val position = partnerPosition(
        myLocation = snapshot?.myLocation,
        partnerLocation = snapshot?.partnerLocation?.takeIf { paired },
        sharingEnabled = snapshot?.sharing?.enabled == true,
        now = now,
    )
    return WidgetState(
        distance = position.distanceKm?.let { formatDistance(it, distanceUnit, locale) },
        distanceUnit = distanceUnit,
        freshness = position.freshness,
        daysUntilReunion = snapshot?.reunion?.takeIf { paired }?.let { countdownUntil(it.meetAt, now).days },
    )
}
