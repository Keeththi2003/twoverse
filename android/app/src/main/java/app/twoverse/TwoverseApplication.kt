package app.twoverse

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import app.twoverse.core.data.location.LocationSharingController
import app.twoverse.core.data.push.PushRegistrar
import app.twoverse.core.data.push.TwoverseNotifications
import app.twoverse.core.data.push.WakeUpPinger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TwoverseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var locationSharingController: LocationSharingController

    @Inject
    lateinit var pushRegistrar: PushRegistrar

    @Inject
    lateinit var wakeUpPinger: WakeUpPinger

    @Inject
    lateinit var notifications: TwoverseNotifications

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notifications.createChannels()
        locationSharingController.start()
        pushRegistrar.start()
        wakeUpPinger.start()
    }
}
