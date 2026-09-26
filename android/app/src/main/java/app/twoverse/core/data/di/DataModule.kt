package app.twoverse.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.BirthdayRepository
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.MemoryRepository
import app.twoverse.core.data.ReunionRepository
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.fake.FakeAuthRepository
import app.twoverse.core.data.fake.FakeBirthdayRepository
import app.twoverse.core.data.fake.FakeCoupleRepository
import app.twoverse.core.data.fake.FakeLocationRepository
import app.twoverse.core.data.fake.FakeMemoryRepository
import app.twoverse.core.data.fake.FakeReunionRepository
import app.twoverse.core.data.fake.FakeSettingsRepository
import app.twoverse.core.data.local.DataStoreUserPreferences
import app.twoverse.core.data.local.UserPreferences
import app.twoverse.core.data.network.ConnectivityNetworkMonitor
import app.twoverse.core.data.network.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the fake repositories (until Supabase is added) and device-local data sources. */
@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    @Singleton
    fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds
    @Singleton
    fun bindCoupleRepository(impl: FakeCoupleRepository): CoupleRepository

    @Binds
    @Singleton
    fun bindLocationRepository(impl: FakeLocationRepository): LocationRepository

    @Binds
    @Singleton
    fun bindReunionRepository(impl: FakeReunionRepository): ReunionRepository

    @Binds
    @Singleton
    fun bindMemoryRepository(impl: FakeMemoryRepository): MemoryRepository

    @Binds
    @Singleton
    fun bindSettingsRepository(impl: FakeSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    fun bindBirthdayRepository(impl: FakeBirthdayRepository): BirthdayRepository

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
