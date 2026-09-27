package app.twoverse.core.data.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.di.ApplicationScope
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Receives the batched "moved about 1 km" updates and uploads the latest one (FR-LOC-4). */
@AndroidEntryPoint
class SignificantMovementReceiver : BroadcastReceiver() {

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val position = LocationResult.extractResult(intent)?.lastLocation?.toPosition() ?: return
        val pending = goAsync()
        appScope.launch {
            try {
                locationRepository.upload(position)
            } finally {
                pending.finish()
            }
        }
    }
}
