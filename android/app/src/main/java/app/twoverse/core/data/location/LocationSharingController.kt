package app.twoverse.core.data.location

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import app.twoverse.core.data.LocationPermissionChecker
import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps location sharing running to match the user's choice: frequent updates while the app
 * is open, the background schedule otherwise. Permissions are re-checked every time the app
 * comes to the foreground, since the user can change them in system settings.
 */
@Singleton
class LocationSharingController @Inject constructor(
    private val locationRepository: LocationRepository,
    private val locationSource: FusedLocationSource,
    private val backgroundScheduler: BackgroundLocationScheduler,
    private val permissions: LocationPermissionChecker,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        val appInForeground = ProcessLifecycleOwner.get().lifecycle.currentStateFlow
            .map { it.isAtLeast(Lifecycle.State.STARTED) }
            .distinctUntilChanged()
        appScope.launch {
            combine(appInForeground, locationRepository.sharing) { foreground, sharing ->
                Triple(sharingMode(foreground, sharing, permissions.status()), sharing, foreground)
            }
                .distinctUntilChanged()
                .collectLatest { (mode, sharing) ->
                    if (mode.runInBackground) backgroundScheduler.start() else backgroundScheduler.stop()
                    if (mode.trackInForeground) {
                        locationSource.foregroundUpdates(sharing.precision).collect { locationRepository.upload(it) }
                    }
                }
        }
    }
}
