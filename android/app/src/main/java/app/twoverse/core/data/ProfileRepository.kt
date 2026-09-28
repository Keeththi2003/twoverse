package app.twoverse.core.data

import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.ProfileSettings
import app.twoverse.core.model.UserProfile

interface ProfileRepository {
    /** The profile of the signed-in user or their partner; others are not readable. */
    suspend fun profile(userId: String): DataResult<UserProfile>

    /** Saves the signed-in user's time zone, so reunion-day notifications arrive on their local day (FR-NOT-4). */
    suspend fun updateTimeZone(zoneId: String): DataResult<Unit>

    /** The signed-in user's settings saved on the server (FR-VLT-5, FR-SET-3). */
    suspend fun settings(): DataResult<ProfileSettings>

    suspend fun setLockOurs(enabled: Boolean): DataResult<Unit>

    suspend fun setDistanceUnit(unit: DistanceUnit): DataResult<Unit>

    suspend fun setAppearance(appearance: AppearanceMode): DataResult<Unit>
}
