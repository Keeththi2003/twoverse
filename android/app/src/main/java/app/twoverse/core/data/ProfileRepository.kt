package app.twoverse.core.data

import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.MyProfile
import app.twoverse.core.model.ProfileEdit
import app.twoverse.core.model.ProfileSettings
import app.twoverse.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

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

    /**
     * The signed-in user's own profile, with their phone and sharing choices (FR-PRO-4). Waits
     * while it can't be loaded; null when signed out. Updates after their own changes.
     */
    val myProfile: Flow<MyProfile?>

    suspend fun updateMyProfile(edit: ProfileEdit): DataResult<Unit>

    /** Starts an email change: the new address gets a confirmation email; the old one stays until then. */
    suspend fun changeEmail(newEmail: String): DataResult<Unit>

    /**
     * The partner's names and pronouns, their email and phone only if they share them, and my
     * nickname for them (FR-PRO-5); null when unpaired.
     */
    suspend fun partnerProfile(): DataResult<UserProfile?>

    /** Sets, or with null clears, my private nickname for the partner (FR-PRO-1). */
    suspend fun setPartnerNickname(nickname: String?): DataResult<Unit>
}
