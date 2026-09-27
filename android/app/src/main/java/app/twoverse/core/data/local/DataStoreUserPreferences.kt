package app.twoverse.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DistanceUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreUserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferences {

    override val appearance: Flow<AppearanceMode> = dataStore.data
        .map { prefs -> enumOrDefault(prefs[AppearanceKey], AppearanceMode.System) }
        .distinctUntilChanged()

    override val distanceUnit: Flow<DistanceUnit> = dataStore.data
        .map { prefs -> enumOrDefault(prefs[DistanceUnitKey], DistanceUnit.Kilometres) }
        .distinctUntilChanged()

    override val birthdayWelcomeSeen: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[BirthdayWelcomeSeenKey] ?: false }
        .distinctUntilChanged()

    override val lockOurs: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[LockOursKey] ?: true }
        .distinctUntilChanged()

    override val batteryGuideShown: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[BatteryGuideShownKey] ?: false }
        .distinctUntilChanged()

    override val notificationPermissionAsked: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[NotificationPermissionAskedKey] ?: false }
        .distinctUntilChanged()

    override suspend fun setAppearance(appearance: AppearanceMode) {
        dataStore.edit { it[AppearanceKey] = appearance.name }
    }

    override suspend fun setDistanceUnit(unit: DistanceUnit) {
        dataStore.edit { it[DistanceUnitKey] = unit.name }
    }

    override suspend fun setBirthdayWelcomeSeen() {
        dataStore.edit { it[BirthdayWelcomeSeenKey] = true }
    }

    override suspend fun setLockOurs(enabled: Boolean) {
        dataStore.edit { it[LockOursKey] = enabled }
    }

    override suspend fun setBatteryGuideShown() {
        dataStore.edit { it[BatteryGuideShownKey] = true }
    }

    override suspend fun setNotificationPermissionAsked() {
        dataStore.edit { it[NotificationPermissionAskedKey] = true }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        val AppearanceKey = stringPreferencesKey("appearance")
        val DistanceUnitKey = stringPreferencesKey("distance_unit")
        val BirthdayWelcomeSeenKey = booleanPreferencesKey("birthday_welcome_seen")

        /** Lock Ours is on by default (FR-VLT-5). */
        val LockOursKey = booleanPreferencesKey("lock_ours")
        val BatteryGuideShownKey = booleanPreferencesKey("battery_guide_shown")
        val NotificationPermissionAskedKey = booleanPreferencesKey("notification_permission_asked")
    }
}
