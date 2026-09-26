package app.twoverse.widget

import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.model.DistanceUnit

/** What the widget shows (FR-WGT-1). Never photos or captions (FR-WGT-3). */
data class WidgetState(
    /** Formatted distance number, or null when unavailable. */
    val distance: String?,
    val distanceUnit: DistanceUnit,
    val freshness: LocationFreshness,
    /** Whole days until the reunion, or null when no date is set. */
    val daysUntilReunion: Long?,
)
