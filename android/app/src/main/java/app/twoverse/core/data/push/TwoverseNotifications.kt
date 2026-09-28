package app.twoverse.core.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.twoverse.R
import app.twoverse.core.common.EXTRA_LAUNCH_SCREEN
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Notification channels and the notifications shown for pushes (FR-NOT). */
@Singleton
class TwoverseNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private enum class Channel(
        val id: String,
        @param:StringRes val nameRes: Int,
        @param:StringRes val descriptionRes: Int,
        val importance: Int,
    ) {
        Partner("partner", R.string.notification_channel_partner, R.string.notification_channel_partner_description, NotificationManager.IMPORTANCE_DEFAULT),
        Reunion("reunion", R.string.notification_channel_reunion, R.string.notification_channel_reunion_description, NotificationManager.IMPORTANCE_HIGH),
        Memories("memories", R.string.notification_channel_memories, R.string.notification_channel_memories_description, NotificationManager.IMPORTANCE_HIGH),
    }

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        Channel.entries.forEach { channel ->
            manager.createNotificationChannel(
                NotificationChannel(channel.id, context.getString(channel.nameRes), channel.importance).apply {
                    description = context.getString(channel.descriptionRes)
                },
            )
        }
    }

    /** Shows the notification for a visible push; its text never includes content from the push (FR-NOT-1). */
    fun show(push: IncomingPush) {
        val (channel, titleRes, bodyRes) = when (push) {
            IncomingPush.WakeUp -> return
            IncomingPush.PartnerJoined -> Triple(Channel.Partner, R.string.notification_partner_joined_title, R.string.notification_partner_joined_body)
            IncomingPush.ReunionDay -> Triple(Channel.Reunion, R.string.notification_reunion_day_title, R.string.notification_reunion_day_body)
            IncomingPush.NewMemory -> Triple(Channel.Memories, R.string.notification_new_memory_title, R.string.notification_new_memory_body)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.brand_primary))
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(bodyRes))
            .setAutoCancel(true)
            .setContentIntent(openIntent(push))
            .build()
        NotificationManagerCompat.from(context).notify(push.ordinal, notification)
    }

    /** Opens the app on the screen that fits the notification. */
    private fun openIntent(push: IncomingPush): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launch.putExtra(EXTRA_LAUNCH_SCREEN, push.opens?.name)
        return PendingIntent.getActivity(context, push.ordinal, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
