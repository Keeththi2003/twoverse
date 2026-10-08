package app.twoverse.testing

import app.twoverse.core.data.local.UserPreferences
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DistanceUnit
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/** [UserPreferences] without DataStore, for JVM tests. */
class InMemoryUserPreferences : UserPreferences {
    override val appearance = MutableStateFlow(AppearanceMode.System)
    override val distanceUnit = MutableStateFlow(DistanceUnit.Kilometres)
    override val lockOurs = MutableStateFlow(true)
    override val batteryGuideShown = MutableStateFlow(false)
    override val notificationPermissionAsked = MutableStateFlow(false)
    override val dismissedMeetupQuestion = MutableStateFlow<Instant?>(null)

    override suspend fun setAppearance(appearance: AppearanceMode) {
        this.appearance.value = appearance
    }

    override suspend fun setDistanceUnit(unit: DistanceUnit) {
        distanceUnit.value = unit
    }

    override suspend fun setLockOurs(enabled: Boolean) {
        lockOurs.value = enabled
    }

    override suspend fun setBatteryGuideShown() {
        batteryGuideShown.value = true
    }

    override suspend fun setNotificationPermissionAsked() {
        notificationPermissionAsked.value = true
    }

    override suspend fun setDismissedMeetupQuestion(reunionAt: Instant) {
        dismissedMeetupQuestion.value = reunionAt
    }
}
