package app.twoverse.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import app.twoverse.MainActivity
import app.twoverse.core.common.EXTRA_LAUNCH_SCREEN
import app.twoverse.core.common.ticks
import app.twoverse.core.model.LaunchScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Home-screen widget: distance, freshness and days until the reunion (FR-WGT-1 to FR-WGT-5). */
class TwoverseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = stateFlow(EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java))
        val openHome = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_LAUNCH_SCREEN, LaunchScreen.Home.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        provideContent {
            val current by state.collectAsState(initial = null)
            TwoverseWidgetContent(state = current, openHome = openHome)
        }
    }

    /** Saved data only: no network, so it also works offline (FR-WGT-2, NFR-REL-1). */
    private fun stateFlow(entryPoint: WidgetEntryPoint): Flow<WidgetState> {
        val clock = entryPoint.clock()
        return combine(
            entryPoint.offlineCache().snapshot,
            entryPoint.userPreferences().distanceUnit,
            clock.ticks(RefreshMillis),
        ) { snapshot, unit, now -> widgetState(snapshot, unit, now) }
    }

    private companion object {
        const val RefreshMillis = 60_000L
    }
}

class TwoverseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TwoverseWidget()
}
