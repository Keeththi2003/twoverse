package app.twoverse.core.data.fake

import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeSettingsRepository @Inject constructor() : SettingsRepository {
    private val current = MutableStateFlow(SampleData.settings)

    override val settings: StateFlow<UserSettings> = current

    override suspend fun setShareLocation(enabled: Boolean) {
        current.update { it.copy(shareLocation = enabled) }
    }

    override suspend fun setLocationPrecision(precision: LocationPrecision) {
        current.update { it.copy(locationPrecision = precision) }
    }

    override suspend fun setLockOurs(enabled: Boolean) {
        current.update { it.copy(lockOurs = enabled) }
    }

    override suspend fun setDistanceUnit(unit: DistanceUnit) {
        current.update { it.copy(distanceUnit = unit) }
    }

    override suspend fun setAppearance(appearance: AppearanceMode) {
        current.update { it.copy(appearance = appearance) }
    }
}
