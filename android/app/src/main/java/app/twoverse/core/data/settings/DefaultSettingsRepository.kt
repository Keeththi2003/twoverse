package app.twoverse.core.data.settings

import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.local.UserPreferences
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Location sharing and precision are stored with the user's location on the server
 * (FR-LOC-2, FR-LOC-6); the other settings are device preferences in DataStore.
 */
@Singleton
class DefaultSettingsRepository @Inject constructor(
    private val locationRepository: LocationRepository,
    private val preferences: UserPreferences,
) : SettingsRepository {

    override val settings: Flow<UserSettings> = combine(
        locationRepository.sharing,
        preferences.lockOurs,
        preferences.distanceUnit,
        preferences.appearance,
    ) { sharing, lockOurs, unit, appearance ->
        UserSettings(
            shareLocation = sharing.enabled,
            locationPrecision = sharing.precision,
            lockOurs = lockOurs,
            distanceUnit = unit,
            appearance = appearance,
        )
    }

    override suspend fun setShareLocation(enabled: Boolean): DataResult<Unit> =
        locationRepository.setSharingEnabled(enabled)

    override suspend fun setLocationPrecision(precision: LocationPrecision): DataResult<Unit> =
        locationRepository.setPrecision(precision)

    override suspend fun setLockOurs(enabled: Boolean) {
        preferences.setLockOurs(enabled)
    }

    override suspend fun setDistanceUnit(unit: DistanceUnit) {
        preferences.setDistanceUnit(unit)
    }

    override suspend fun setAppearance(appearance: AppearanceMode) {
        preferences.setAppearance(appearance)
    }
}
