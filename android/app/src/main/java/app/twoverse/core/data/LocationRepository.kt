package app.twoverse.core.data

import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DevicePosition
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.LocationSharing
import app.twoverse.core.model.UserLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    /** The user's own latest uploaded position, or null when not sharing or not known yet. */
    val myLocation: Flow<UserLocation?>

    /** The partner's latest position, live; null when they don't share it or there is no data. */
    val partnerLocation: Flow<UserLocation?>

    /** The user's sharing choice, stored on the server so it survives restarts (FR-LOC-2). */
    val sharing: Flow<LocationSharing>

    /** Turning sharing off also removes the stored position (BR-3). */
    suspend fun setSharingEnabled(enabled: Boolean): DataResult<Unit>

    suspend fun setPrecision(precision: LocationPrecision): DataResult<Unit>

    /**
     * Uploads a new position, rounded for the chosen precision (FR-LOC-6). Replaces the
     * previous one; no history is kept (FR-LOC-7). Does nothing while sharing is off.
     */
    suspend fun upload(position: DevicePosition): DataResult<Unit>
}
