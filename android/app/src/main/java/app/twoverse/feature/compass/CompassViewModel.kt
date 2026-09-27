package app.twoverse.feature.compass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.PartnerPosition
import app.twoverse.core.common.formatDistance
import app.twoverse.core.common.isPointingAt
import app.twoverse.core.common.partnerPosition
import app.twoverse.core.common.ticks
import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.sensors.CompassHeading
import app.twoverse.core.model.HeadingSample
import app.twoverse.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class CompassViewModel @Inject constructor(
    locationRepository: LocationRepository,
    settingsRepository: SettingsRepository,
    compassHeading: CompassHeading,
    clock: Clock,
) : ViewModel() {

    private val snapshot: Flow<Snapshot> = combine(
        locationRepository.myLocation,
        locationRepository.partnerLocation,
        settingsRepository.settings,
        clock.ticks(),
    ) { mine, partner, settings, now ->
        Snapshot(partnerPosition(mine, partner, settings.shareLocation, now), settings)
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    /** Sensor readings only while the screen is visible (collected with the lifecycle). */
    private val heading: Flow<HeadingSample?> = compassHeading.headings(locationRepository.myLocation)
        .map<HeadingSample, HeadingSample?> { it }
        .onStart { emit(null) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    private val hasCompassSensor = compassHeading.isAvailable

    val uiState: StateFlow<CompassUiState> = combine(snapshot, heading) { snapshot, heading ->
        val position = snapshot.position
        val bearing = position.bearingDegrees
        CompassUiState.Success(
            distance = position.distanceKm?.let { formatDistance(it, snapshot.settings.distanceUnit) },
            distanceUnit = snapshot.settings.distanceUnit,
            direction = bearing?.let(CompassDirection::fromBearing),
            bearingDegrees = bearing?.roundToInt()?.rem(FullCircle),
            showsNeedle = bearing != null && hasCompassSensor,
            hasCompassSensor = hasCompassSensor,
            isPointingAtPartner = bearing != null && heading != null && isPointingAt(bearing, heading.degrees),
            freshness = position.freshness,
            partnerUpdatedAgo = position.updatedAgo,
            unavailableReason = position.unavailableReason,
            isCalibrated = heading?.isAccurate ?: true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), CompassUiState.Loading)

    /**
     * Needle angle on screen, clockwise from the top: the partner's bearing minus where the
     * phone points (FR-CMP-3). Continuous (not wrapped to 0..360) so it never spins backwards.
     * Kept apart from [uiState] because it changes about 50 times a second.
     */
    val needleRotation: StateFlow<Float?> = combine(snapshot, heading) { snapshot, heading ->
        snapshot.position.bearingDegrees?.let { bearing -> (bearing - (heading?.degrees ?: 0.0)).toFloat() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private data class Snapshot(val position: PartnerPosition, val settings: UserSettings)

    private companion object {
        const val FullCircle = 360
    }
}
