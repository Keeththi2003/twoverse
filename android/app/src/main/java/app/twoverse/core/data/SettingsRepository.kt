package app.twoverse.core.data

import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>

    /** Saved on the server (FR-LOC-2). */
    suspend fun setShareLocation(enabled: Boolean): DataResult<Unit>

    /** Saved on the server (FR-LOC-6). */
    suspend fun setLocationPrecision(precision: LocationPrecision): DataResult<Unit>

    suspend fun setLockOurs(enabled: Boolean)

    suspend fun setDistanceUnit(unit: DistanceUnit)

    suspend fun setAppearance(appearance: AppearanceMode)
}
