package app.twoverse.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.data.local.OfflineCache
import app.twoverse.core.data.local.UserPreferences
import app.twoverse.core.model.DistanceUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Redraws the widget whenever the saved location, sharing or reunion data changes (FR-WGT-2).
 * Between changes the system refreshes it every 30 minutes so freshness keeps ageing.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineCache: OfflineCache,
    private val preferences: UserPreferences,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    @OptIn(FlowPreview::class)
    fun start() {
        appScope.launch {
            combine(offlineCache.snapshot, preferences.distanceUnit) { snapshot, unit ->
                WidgetInputs(
                    paired = snapshot?.couple != null,
                    sharing = snapshot?.sharing?.enabled,
                    myUpdatedAt = snapshot?.myLocation?.updatedAt,
                    partnerUpdatedAt = snapshot?.partnerLocation?.updatedAt,
                    partnerPosition = snapshot?.partnerLocation?.let { it.latitude to it.longitude },
                    reunionAt = snapshot?.reunion?.meetAt,
                    unit = unit,
                )
            }
                .distinctUntilChanged()
                .drop(1)
                .debounce(DebounceMillis)
                .collect { TwoverseWidget().updateAll(context) }
        }
    }

    /** Only what the widget shows; other saved data (like memories) never redraws it. */
    private data class WidgetInputs(
        val paired: Boolean,
        val sharing: Boolean?,
        val myUpdatedAt: Instant?,
        val partnerUpdatedAt: Instant?,
        val partnerPosition: Pair<Double, Double>?,
        val reunionAt: Instant?,
        val unit: DistanceUnit,
    )

    private companion object {
        const val DebounceMillis = 1_000L
    }
}
