package app.twoverse

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import app.twoverse.core.data.local.OursLock
import app.twoverse.core.data.local.SignedOutCleanup
import app.twoverse.core.data.location.LocationSharingController
import app.twoverse.core.data.push.PushRegistrar
import app.twoverse.core.data.push.TwoverseNotifications
import app.twoverse.core.data.push.WakeUpPinger
import app.twoverse.core.data.settings.ProfileSettingsSync
import app.twoverse.widget.WidgetUpdater
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

    @Inject
    lateinit var profileSettingsSync: ProfileSettingsSync

    @Inject
    lateinit var signedOutCleanup: SignedOutCleanup

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    @Inject
    lateinit var oursLock: OursLock

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notifications.createChannels()
        locationSharingController.start()
        pushRegistrar.start()
        wakeUpPinger.start()
        profileSettingsSync.start()
        signedOutCleanup.start()
        widgetUpdater.start()
        oursLock.start()
    }
}
