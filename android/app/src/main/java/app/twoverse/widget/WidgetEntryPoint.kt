package app.twoverse.widget

import app.twoverse.core.data.local.OfflineCache
import app.twoverse.core.data.local.UserPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** Glance widgets aren't Hilt entry points, so the widget pulls its dependencies from here. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun offlineCache(): OfflineCache
    fun userPreferences(): UserPreferences
    fun clock(): Clock
}
