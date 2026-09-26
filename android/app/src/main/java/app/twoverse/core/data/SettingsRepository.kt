package app.twoverse.core.data

import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setShareLocation(enabled: Boolean)

    suspend fun setLocationPrecision(precision: LocationPrecision)

    suspend fun setLockOurs(enabled: Boolean)

    suspend fun setDistanceUnit(unit: DistanceUnit)

    suspend fun setAppearance(appearance: AppearanceMode)
}
