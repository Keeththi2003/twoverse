package app.twoverse.core.data.local

import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DistanceUnit
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Display preferences stored on this device (DataStore), so they survive restarts. */
interface UserPreferences {
    val appearance: Flow<AppearanceMode>
    val distanceUnit: Flow<DistanceUnit>
    val lockOurs: Flow<Boolean>

    /** The battery-settings guide is shown once, after sharing is first turned on. */
    val batteryGuideShown: Flow<Boolean>

    suspend fun setAppearance(appearance: AppearanceMode)

    suspend fun setDistanceUnit(unit: DistanceUnit)

    suspend fun setLockOurs(enabled: Boolean)

    suspend fun setBatteryGuideShown()

    /** Android 13+ asks once for notification permission (FR-NOT). */
    val notificationPermissionAsked: Flow<Boolean>

    suspend fun setNotificationPermissionAsked()

    /** The passed reunion whose "Did you meet?" question was answered No on this device (FR-ORB-10). */
    val dismissedMeetupQuestion: Flow<Instant?>

    suspend fun setDismissedMeetupQuestion(reunionAt: Instant)
}
