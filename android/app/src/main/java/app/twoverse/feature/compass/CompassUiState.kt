package app.twoverse.feature.compass

import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.common.LocationUnavailableReason
import app.twoverse.core.model.DistanceUnit

sealed interface CompassUiState {
    data object Loading : CompassUiState

    data class Success(
        /** Formatted distance number ("94.6"), or null when unavailable. */
        val distance: String?,
        val distanceUnit: DistanceUnit,
        val direction: CompassDirection?,
        /** Bearing to the partner in whole degrees from true north (FR-CMP-4). */
        val bearingDegrees: Int?,
        /** Needle angle on screen: bearing minus device heading. Null hides the needle (FR-CMP-6). */
        val needleRotation: Float?,
        val freshness: LocationFreshness,
        val partnerUpdatedAgo: ElapsedTime?,
        val unavailableReason: LocationUnavailableReason?,
        /** False when sensor accuracy is low, which shows the calibration hint (FR-CMP-5). */
        val isCalibrated: Boolean,
    ) : CompassUiState {
        val isLastKnownDirection: Boolean get() = freshness == LocationFreshness.Outdated
    }
}
