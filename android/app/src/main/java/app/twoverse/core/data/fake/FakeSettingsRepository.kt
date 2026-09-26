package app.twoverse.core.data.fake

import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.local.UserPreferences
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Privacy settings are kept in memory until Supabase exists; appearance and distance unit are
 * device preferences saved in DataStore through [UserPreferences].
 */
@Singleton
class FakeSettingsRepository @Inject constructor(
    private val preferences: UserPreferences,
) : SettingsRepository {
    private val privacy = MutableStateFlow(SampleData.settings)

    override val settings: Flow<UserSettings> = combine(
        privacy,
        preferences.appearance,
        preferences.distanceUnit,
    ) { settings, appearance, unit -> settings.copy(appearance = appearance, distanceUnit = unit) }

    override suspend fun setShareLocation(enabled: Boolean) {
        privacy.update { it.copy(shareLocation = enabled) }
    }

    override suspend fun setLocationPrecision(precision: LocationPrecision) {
        privacy.update { it.copy(locationPrecision = precision) }
    }

    override suspend fun setLockOurs(enabled: Boolean) {
        privacy.update { it.copy(lockOurs = enabled) }
    }

    override suspend fun setDistanceUnit(unit: DistanceUnit) {
        preferences.setDistanceUnit(unit)
    }

    override suspend fun setAppearance(appearance: AppearanceMode) {
        preferences.setAppearance(appearance)
    }
}
