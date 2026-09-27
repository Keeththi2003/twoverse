package app.twoverse.core.data.location

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Starts or stops background sharing: WorkManager every 15 min plus significant-movement updates. */
@Singleton
class BackgroundLocationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationSource: FusedLocationSource,
) {
    private val movementIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, SignificantMovementReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    fun start() {
        val request = PeriodicWorkRequestBuilder<LocationUploadWorker>(IntervalMinutes, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WorkName, ExistingPeriodicWorkPolicy.KEEP, request)
        locationSource.startSignificantMovementUpdates(movementIntent)
    }

    fun stop() {
        WorkManager.getInstance(context).cancelUniqueWork(WorkName)
        locationSource.stopSignificantMovementUpdates(movementIntent)
    }

    private companion object {
        const val WorkName = "twoverse-location-upload"
        const val IntervalMinutes = 15L
    }
}
