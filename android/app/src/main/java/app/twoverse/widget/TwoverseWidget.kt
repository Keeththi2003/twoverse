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
import app.twoverse.core.common.countdownUntil
import app.twoverse.core.common.formatDistance
import app.twoverse.core.common.partnerPosition
import app.twoverse.core.common.ticks
import app.twoverse.core.model.LaunchScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Home-screen widget: distance, freshness and days until the reunion (FR-WGT-1 to FR-WGT-5). */
class TwoverseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = widgetState(EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java))
        val openHome = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_LAUNCH_SCREEN, LaunchScreen.Home.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        provideContent {
            val current by state.collectAsState(initial = null)
            TwoverseWidgetContent(state = current, openHome = openHome)
        }
    }

    /** Updates while the widget session is active, as new location or countdown data arrives (FR-WGT-2). */
    private fun widgetState(entryPoint: WidgetEntryPoint): Flow<WidgetState> {
        val location = entryPoint.locationRepository()
        return combine(
            location.myLocation,
            location.partnerLocation,
            entryPoint.settingsRepository().settings,
            entryPoint.reunionRepository().reunion,
            entryPoint.clock().ticks(RefreshMillis),
        ) { mine, partner, settings, reunion, now ->
            val position = partnerPosition(mine, partner, settings.shareLocation, now)
            WidgetState(
                distance = position.distanceKm?.let { formatDistance(it, settings.distanceUnit) },
                distanceUnit = settings.distanceUnit,
                freshness = position.freshness,
                daysUntilReunion = reunion?.let { countdownUntil(it.meetAt, now).days },
            )
        }
    }

    private companion object {
        const val RefreshMillis = 60_000L
    }
}

class TwoverseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TwoverseWidget()
}
