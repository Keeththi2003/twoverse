package app.twoverse.core.data.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/** Receives pushes from send-push. Runs on a background thread, never the main thread. */
@AndroidEntryPoint
class TwoverseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var registrar: PushRegistrar

    @Inject
    lateinit var notifications: TwoverseNotifications

    @Inject
    lateinit var wakeUpHandler: WakeUpHandler

    override fun onNewToken(token: String) {
        registrar.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        when (val push = IncomingPush.fromType(message.data["type"])) {
            null -> Unit
            // The wake-up must finish before this returns, while the system keeps the app awake.
            IncomingPush.WakeUp -> runBlocking { wakeUpHandler.handle() }
            else -> notifications.show(push)
        }
    }
}
