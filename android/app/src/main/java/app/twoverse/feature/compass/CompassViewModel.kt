package app.twoverse.feature.compass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.formatDistance
import app.twoverse.core.common.partnerPosition
import app.twoverse.core.common.ticks
import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class CompassViewModel @Inject constructor(
    locationRepository: LocationRepository,
    settingsRepository: SettingsRepository,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<CompassUiState> = combine(
        locationRepository.myLocation,
        locationRepository.partnerLocation,
        settingsRepository.settings,
        clock.ticks(),
    ) { mine, partner, settings, now ->
        val position = partnerPosition(mine, partner, settings.shareLocation, now)
        val bearing = position.bearingDegrees
        CompassUiState.Success(
            distance = position.distanceKm?.let { formatDistance(it, settings.distanceUnit) },
            distanceUnit = settings.distanceUnit,
            direction = bearing?.let(CompassDirection::fromBearing),
            bearingDegrees = bearing?.roundToInt()?.rem(FullCircle),
            needleRotation = bearing?.let { (it - DeviceHeadingDegrees).toFloat() },
            freshness = position.freshness,
            partnerUpdatedAgo = position.updatedAgo,
            unavailableReason = position.unavailableReason,
            isCalibrated = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = CompassUiState.Loading,
    )

    private companion object {
        /**
         * Until the rotation-vector sensor is added (FR-CMP-2), the phone is treated as facing
         * true north with good accuracy, so the needle shows the raw bearing.
         */
        const val DeviceHeadingDegrees = 0.0
        const val FullCircle = 360
        const val StopTimeoutMillis = 5_000L
    }
}
