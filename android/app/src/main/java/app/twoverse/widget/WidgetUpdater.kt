package app.twoverse.widget

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.data.local.OfflineCache
import app.twoverse.core.data.local.UserPreferences
import app.twoverse.core.data.network.NetworkMonitor
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.isNew
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Redraws the widget whenever what it shows changes: location, sharing, reunion, new memories,
 * a waiting Shooting Star, Our Orbit, connectivity or the Appearance setting (FR-WGT-2, FR-WGT-5,
 * FR-WGT-8), and when the phone switches dark mode. Between changes the system refreshes it every
 * 30 minutes so freshness keeps ageing. Also publishes the generated picker preview.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineCache: OfflineCache,
    private val preferences: UserPreferences,
    private val networkMonitor: NetworkMonitor,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    @OptIn(FlowPreview::class)
    fun start() {
        appScope.launch {
            combine(
                offlineCache.snapshot,
                preferences.distanceUnit,
                preferences.appearance,
                networkMonitor.isOnline,
            ) { snapshot, unit, appearance, online ->
                WidgetInputs(
                    paired = snapshot?.couple != null,
                    partnerTimeZone = snapshot?.couple?.partner?.timeZone,
                    sharing = snapshot?.sharing?.enabled,
                    myUpdatedAt = snapshot?.myLocation?.updatedAt,
                    partnerUpdatedAt = snapshot?.partnerLocation?.updatedAt,
                    partnerPosition = snapshot?.partnerLocation?.let { it.latitude to it.longitude },
                    reunionAt = snapshot?.reunion?.meetAt,
                    reunionSetAt = snapshot?.reunion?.dateSetAt,
                    newMemories = snapshot?.memories?.count { it.isNew } ?: 0,
                    hasWaitingStar = snapshot?.hasWaitingStar == true,
                    togetherSince = snapshot?.couple?.togetherSince,
                    timesMet = snapshot?.meetups?.size ?: 0,
                    unit = unit,
                    appearance = appearance,
                    online = online,
                )
            }
                .distinctUntilChanged()
                .drop(1)
                .debounce(DebounceMillis)
                .collect { TwoverseWidget().updateAll(context) }
        }
        redrawOnDarkModeChange()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // The system rate-limits this; a refused call just keeps the preview published earlier.
            appScope.launch { GlanceAppWidgetManager(context).setWidgetPreviews(TwoverseWidgetReceiver::class) }
        }
    }

    /**
     * Following the system, Android 12+ launchers switch the widget's day/night colours themselves;
     * older versions only pick them when the widget is drawn, so it is redrawn here as well.
     */
    private fun redrawOnDarkModeChange() {
        var nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        context.registerComponentCallbacks(
            object : ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: Configuration) {
                    val newNightMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    if (newNightMode == nightMode) return
                    nightMode = newNightMode
                    appScope.launch { TwoverseWidget().updateAll(context) }
                }

                @Deprecated("Deprecated in Java")
                override fun onLowMemory() = Unit
            },
        )
    }

    /** Only what the widget shows; other saved data (like captions) never redraws it. */
    private data class WidgetInputs(
        val paired: Boolean,
        val partnerTimeZone: String?,
        val sharing: Boolean?,
        val myUpdatedAt: Instant?,
        val partnerUpdatedAt: Instant?,
        val partnerPosition: Pair<Double, Double>?,
        val reunionAt: Instant?,
        val reunionSetAt: Instant?,
        val newMemories: Int,
        val hasWaitingStar: Boolean,
        val togetherSince: LocalDate?,
        val timesMet: Int,
        val unit: DistanceUnit,
        val appearance: AppearanceMode,
        val online: Boolean,
    )

    private companion object {
        const val DebounceMillis = 1_000L
    }
}
