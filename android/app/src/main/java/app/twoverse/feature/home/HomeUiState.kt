package app.twoverse.feature.home

import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.DayPeriod
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.common.LocationUnavailableReason
import app.twoverse.core.model.DistanceUnit
import java.time.LocalDate

/** Whether this user's location is being shared (FR-LOC-2, NFR-PRV-2). */
enum class SharingStatus { On, Off, PermissionNeeded }

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val dayPeriod: DayPeriod,
        val sharingStatus: SharingStatus,
        /** Formatted distance number ("94.6"), or null when unavailable (FR-LOC-11). */
        val distance: String?,
        val distanceUnit: DistanceUnit,
        val unavailableReason: LocationUnavailableReason?,
        val freshness: LocationFreshness,
        val partnerUpdatedAgo: ElapsedTime?,
        val myCity: String?,
        val partnerCity: String?,
        val partnerDirection: CompassDirection?,
        /** Her bearing from this user in whole degrees from north (FR-CMP-1); null with no direction. */
        val partnerBearing: Int? = null,
        /** Whole days until the reunion, or null when no date is set. */
        val daysUntilReunion: Long?,
        val memoryCount: Int,
        val newMemoryCount: Int,
        /** Days together and times met (FR-ORB-9); null until "together since" is set or has begun. */
        val orbit: HomeOrbit? = null,
        /** "Together since" isn't set yet: a gentle prompt to set it (FR-ORB-2). */
        val askTogetherSince: Boolean = false,
        /** The passed reunion day to ask "Did you meet on …?" about (FR-ORB-10); null when there is nothing to ask. */
        val meetupQuestion: LocalDate? = null,
    ) : HomeUiState
}

data class HomeOrbit(val totalDays: Long, val timesMet: Int)
