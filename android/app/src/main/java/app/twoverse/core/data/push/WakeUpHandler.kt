package app.twoverse.core.data.push

import app.twoverse.core.data.LocationPermissionChecker
import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.location.FusedLocationSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * The partner opened the app and asked for a fresh location (FR-LOC-5). Uploads one silently,
 * only if this user shares their location and allowed it in the background (FR-NOT-5).
 */
class WakeUpHandler @Inject constructor(
    private val locationRepository: LocationRepository,
    private val locationSource: FusedLocationSource,
    private val permissions: LocationPermissionChecker,
) {
    suspend fun handle() {
        if (!permissions.status().background) return
        withTimeoutOrNull(TimeoutMillis) {
            val sharing = locationRepository.sharing.first()
            if (!sharing.enabled) return@withTimeoutOrNull
            locationSource.currentPosition(sharing.precision)?.let { locationRepository.upload(it) }
        }
    }

    private companion object {
        /** FCM gives a high-priority message about 10 seconds of processing time. */
        const val TimeoutMillis = 9_000L
    }
}
