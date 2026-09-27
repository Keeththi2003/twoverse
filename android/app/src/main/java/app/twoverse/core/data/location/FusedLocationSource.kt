package app.twoverse.core.data.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.os.Looper
import app.twoverse.core.model.DevicePosition
import app.twoverse.core.model.LocationPrecision
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * The phone's position from the Fused Location Provider. Callers check permissions first
 * ([app.twoverse.core.data.LocationPermissionChecker]); calls without them return nothing.
 */
@Singleton
class FusedLocationSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    /** Updates every few seconds while the app is open (FR-LOC-3). */
    @SuppressLint("MissingPermission")
    fun foregroundUpdates(precision: LocationPrecision): Flow<DevicePosition> = callbackFlow {
        val request = LocationRequest.Builder(precision.priority(), ForegroundIntervalMillis)
            .setMinUpdateIntervalMillis(ForegroundIntervalMillis)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it.toPosition()) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    /** One fresh position for a background upload, or null if none could be found. */
    @SuppressLint("MissingPermission")
    suspend fun currentPosition(precision: LocationPrecision): DevicePosition? =
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            client.getCurrentLocation(precision.priority(), cancellation.token)
                .addOnSuccessListener { location -> continuation.resume(location?.toPosition()) }
                .addOnFailureListener { continuation.resume(null) }
            continuation.invokeOnCancellation { cancellation.cancel() }
        }

    /**
     * Low-power updates delivered to [target] only after moving about 1 km, batched by the
     * system (FR-LOC-4, NFR-BAT-1). No foreground service is needed.
     */
    @SuppressLint("MissingPermission")
    fun startSignificantMovementUpdates(target: PendingIntent) {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, BackgroundIntervalMillis)
            .setMinUpdateDistanceMeters(SignificantMovementMeters)
            .setMaxUpdateDelayMillis(BackgroundIntervalMillis)
            .build()
        client.requestLocationUpdates(request, target)
    }

    fun stopSignificantMovementUpdates(target: PendingIntent) {
        client.removeLocationUpdates(target)
    }

    private fun LocationPrecision.priority(): Int = when (this) {
        LocationPrecision.Precise -> Priority.PRIORITY_HIGH_ACCURACY
        LocationPrecision.Approximate -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
    }

    companion object {
        const val ForegroundIntervalMillis = 5_000L
        const val BackgroundIntervalMillis = 15 * 60_000L
        const val SignificantMovementMeters = 1_000f
    }
}

internal fun android.location.Location.toPosition() = DevicePosition(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracy,
)
