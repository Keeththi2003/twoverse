package app.twoverse.feature.home

import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.DayPeriod
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.common.LocationUnavailableReason
import app.twoverse.core.model.DistanceUnit

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val dayPeriod: DayPeriod,
        val isSharingLocation: Boolean,
        /** Formatted distance number ("94.6"), or null when unavailable (FR-LOC-11). */
        val distance: String?,
        val distanceUnit: DistanceUnit,
        val unavailableReason: LocationUnavailableReason?,
        val freshness: LocationFreshness,
        val partnerUpdatedAgo: ElapsedTime?,
        val myCity: String?,
        val partnerCity: String?,
        val partnerDirection: CompassDirection?,
        val bearingDegrees: Float?,
        /** Whole days until the reunion, or null when no date is set. */
        val daysUntilReunion: Long?,
        val memoryCount: Int,
        val newMemoryCount: Int,
    ) : HomeUiState
}
