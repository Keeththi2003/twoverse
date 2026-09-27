package app.twoverse.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import app.twoverse.core.data.AuthDeepLinkHandler
import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.BirthdayRepository
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.HeadingSource
import app.twoverse.core.data.LocationPermissionChecker
import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.MemoryRepository
import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.data.PushRepository
import app.twoverse.core.data.PushTokenSource
import app.twoverse.core.data.ReunionRepository
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.fake.FakeBirthdayRepository
import app.twoverse.core.data.fake.FakeMemoryRepository
import app.twoverse.core.data.local.DataStoreUserPreferences
import app.twoverse.core.data.location.AndroidLocationPermissionChecker
import app.twoverse.core.data.local.UserPreferences
import app.twoverse.core.data.network.ConnectivityNetworkMonitor
import app.twoverse.core.data.network.NetworkMonitor
import app.twoverse.core.data.sensors.RotationVectorHeadingSource
import app.twoverse.core.data.push.FirebaseTokenSource
import app.twoverse.core.data.settings.DefaultSettingsRepository
import app.twoverse.core.data.supabase.SupabaseAuthDeepLinkHandler
import app.twoverse.core.data.supabase.SupabaseAuthRepository
import app.twoverse.core.data.supabase.SupabaseCoupleRepository
import app.twoverse.core.data.supabase.SupabaseLocationRepository
import app.twoverse.core.data.supabase.SupabaseProfileRepository
import app.twoverse.core.data.supabase.SupabasePushRepository
import app.twoverse.core.data.supabase.SupabaseReunionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Auth, pairing, profiles, location and the reunion use Supabase; the other features still use fakes until
 * they are connected. Device-local preferences, permissions and connectivity are real.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    @Singleton
    fun bindAuthRepository(impl: SupabaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    fun bindCoupleRepository(impl: SupabaseCoupleRepository): CoupleRepository

    @Binds
    @Singleton
    fun bindProfileRepository(impl: SupabaseProfileRepository): ProfileRepository

    @Binds
    @Singleton
    fun bindAuthDeepLinkHandler(impl: SupabaseAuthDeepLinkHandler): AuthDeepLinkHandler

    @Binds
    @Singleton
    fun bindLocationRepository(impl: SupabaseLocationRepository): LocationRepository

    @Binds
    fun bindLocationPermissionChecker(impl: AndroidLocationPermissionChecker): LocationPermissionChecker

    @Binds
    @Singleton
    fun bindReunionRepository(impl: SupabaseReunionRepository): ReunionRepository

    @Binds
    fun bindHeadingSource(impl: RotationVectorHeadingSource): HeadingSource

    @Binds
    @Singleton
    fun bindMemoryRepository(impl: FakeMemoryRepository): MemoryRepository

    @Binds
    @Singleton
    fun bindSettingsRepository(impl: DefaultSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    fun bindBirthdayRepository(impl: FakeBirthdayRepository): BirthdayRepository

    @Binds
    @Singleton
    fun bindPushRepository(impl: SupabasePushRepository): PushRepository

    @Binds
    fun bindPushTokenSource(impl: FirebaseTokenSource): PushTokenSource

    @Binds
    @Singleton
    fun bindUserPreferences(impl: DataStoreUserPreferences): UserPreferences

    @Binds
    @Singleton
    fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    companion object {
        @Provides
        @Singleton
        fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("user_preferences") }
    }
}
