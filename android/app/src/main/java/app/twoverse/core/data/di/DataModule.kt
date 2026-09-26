package app.twoverse.core.data.di

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
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the fake repositories until Supabase is added. */
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
}
