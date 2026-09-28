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

    /** Saved on the server, with a copy on this device so Ours can lock offline (FR-VLT-5). */
    suspend fun setLockOurs(enabled: Boolean): DataResult<Unit>

    /** Saved on the profile and on this device (FR-SET-3). */
    suspend fun setDistanceUnit(unit: DistanceUnit): DataResult<Unit>

    /** Saved on the profile and on this device (FR-SET-3). */
    suspend fun setAppearance(appearance: AppearanceMode): DataResult<Unit>
}
