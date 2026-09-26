package app.twoverse.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
internal object TimeModule {
    /** System clock in the device time zone; tests pass a fixed clock instead. */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
