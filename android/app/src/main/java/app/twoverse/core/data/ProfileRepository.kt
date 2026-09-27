package app.twoverse.core.data

import app.twoverse.core.model.DataResult
import app.twoverse.core.model.UserProfile

interface ProfileRepository {
    /** The profile of the signed-in user or their partner; others are not readable. */
    suspend fun profile(userId: String): DataResult<UserProfile>

    /** Saves the signed-in user's time zone, so reunion-day notifications arrive on their local day (FR-NOT-4). */
    suspend fun updateTimeZone(zoneId: String): DataResult<Unit>
}
