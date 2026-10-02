package app.twoverse.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import app.twoverse.MainActivity
import app.twoverse.core.common.EXTRA_LAUNCH_SCREEN
import app.twoverse.core.common.ticks
import app.twoverse.core.model.LaunchScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Home-screen widget in three sizes (FR-WGT-1 to FR-WGT-7). */
class TwoverseWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(WidgetSizes)

    override val previewSizeMode = SizeMode.Responsive(WidgetSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = stateFlow(EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java))
        val actions = WidgetActions(
            openHome = open(context, LaunchScreen.Home),
            openCountdown = open(context, LaunchScreen.Countdown),
            openVault = open(context, LaunchScreen.Vault),
            openShootingStar = open(context, LaunchScreen.ShootingStar),
            openOrbit = open(context, LaunchScreen.Orbit),
        )
        provideContent {
            val current by state.collectAsState(initial = null)
            TwoverseWidgetContent(state = current, actions = actions)
        }
    }

    /** Generated picker preview with sample data on Android 15+ (FR-WGT-7). */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { TwoverseWidgetContent(state = SampleWidgetState, actions = WidgetActions()) }
    }

    /** Saved data only: no network, so it also works offline (FR-WGT-2, NFR-REL-1). */
    private fun stateFlow(entryPoint: WidgetEntryPoint): Flow<WidgetState> {
        val clock = entryPoint.clock()
        return combine(
            entryPoint.offlineCache().snapshot,
            entryPoint.userPreferences().distanceUnit,
            entryPoint.networkMonitor().isOnline,
            clock.ticks(RefreshMillis),
        ) { snapshot, unit, online, now -> widgetState(snapshot, unit, online, now, clock.zone) }
    }

    private fun open(context: Context, screen: LaunchScreen) = actionStartActivity(
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_LAUNCH_SCREEN, screen.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
    )

    private companion object {
        const val RefreshMillis = 60_000L
    }
}

class TwoverseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TwoverseWidget()
}
