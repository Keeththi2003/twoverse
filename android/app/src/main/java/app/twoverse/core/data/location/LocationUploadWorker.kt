package app.twoverse.core.data.location

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.twoverse.core.data.LocationPermissionChecker
import app.twoverse.core.data.LocationRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Background upload every 15 minutes while sharing is on (FR-LOC-4). */
@HiltWorker
class LocationUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val locationRepository: LocationRepository,
    private val locationSource: FusedLocationSource,
    private val permissions: LocationPermissionChecker,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!permissions.status().background) return Result.success()
        val sharing = locationRepository.sharing.first()
        if (!sharing.enabled) return Result.success()
        val position = locationSource.currentPosition(sharing.precision) ?: return Result.success()
        return when (val result = locationRepository.upload(position)) {
            is DataResult.Success -> Result.success()
            is DataResult.Failure -> if (result.error == DataError.Network) Result.retry() else Result.success()
        }
    }
}
