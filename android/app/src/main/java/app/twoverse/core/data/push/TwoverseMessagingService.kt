package app.twoverse.core.data.push

import app.twoverse.core.common.MaxShortNameLength
import app.twoverse.core.data.local.OfflineCache
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
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

    @Inject
    lateinit var offlineCache: OfflineCache

    override fun onNewToken(token: String) {
        registrar.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        when (val push = IncomingPush.fromType(message.data["type"])) {
            null -> Unit
            // The wake-up must finish before this returns, while the system keeps the app awake.
            IncomingPush.WakeUp -> runBlocking { wakeUpHandler.handle() }
            IncomingPush.ShootingStar -> {
                // Lets the widget say a surprise is waiting, before the app is opened (FR-WGT-1).
                runBlocking { markStarWaiting() }
                notifications.show(push, partnerName = partnerName(message))
            }
            else -> notifications.show(
                push,
                count = message.data["count"]?.toIntOrNull()?.takeIf { it > 0 },
                partnerName = partnerName(message),
            )
        }
    }

    /** How this user knows their partner, resolved by the server (FR-NOT-7); at most 30 characters. */
    private fun partnerName(message: RemoteMessage): String? =
        message.data["partner_name"]?.trim()?.take(MaxShortNameLength)?.takeIf { it.isNotEmpty() }

    private suspend fun markStarWaiting() {
        val ownerId = offlineCache.snapshot.first()?.ownerId ?: return
        offlineCache.update(ownerId) { it.copy(hasWaitingStar = true) }
    }
}
