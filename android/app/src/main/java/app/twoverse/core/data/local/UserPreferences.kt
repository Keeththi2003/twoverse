package app.twoverse.core.data.local

import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DistanceUnit
import kotlinx.coroutines.flow.Flow

/** Display preferences stored on this device (DataStore), so they survive restarts. */
interface UserPreferences {
    val appearance: Flow<AppearanceMode>
    val distanceUnit: Flow<DistanceUnit>

    suspend fun setAppearance(appearance: AppearanceMode)

    suspend fun setDistanceUnit(unit: DistanceUnit)
}
