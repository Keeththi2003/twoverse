package app.twoverse.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.DayPeriod
import app.twoverse.core.common.countdownUntil
import app.twoverse.core.common.formatDistance
import app.twoverse.core.common.partnerPosition
import app.twoverse.core.common.ticks
import app.twoverse.core.data.LocationPermissionChecker
import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.MemoryRepository
import app.twoverse.core.data.ReunionRepository
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender
import app.twoverse.core.model.Reunion
import app.twoverse.core.model.UserLocation
import app.twoverse.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    locationRepository: LocationRepository,
    settingsRepository: SettingsRepository,
    reunionRepository: ReunionRepository,
    memoryRepository: MemoryRepository,
    private val permissions: LocationPermissionChecker,
    private val clock: Clock,
) : ViewModel() {

    private val locations = combine(
        locationRepository.myLocation,
        locationRepository.partnerLocation,
    ) { mine, partner -> mine to partner }

    val uiState: StateFlow<HomeUiState> = combine(
        locations,
        settingsRepository.settings,
        reunionRepository.reunion,
        memoryRepository.memories,
        clock.ticks(),
    ) { (mine, partner), settings, reunion, memories, now ->
        buildState(mine, partner, settings, reunion, memories, now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = HomeUiState.Loading,
    )

    private fun buildState(
        mine: UserLocation?,
        partner: UserLocation?,
        settings: UserSettings,
        reunion: Reunion?,
        memories: List<Memory>,
        now: Instant,
    ): HomeUiState.Success {
        val position = partnerPosition(mine, partner, settings.shareLocation, now)
        return HomeUiState.Success(
            dayPeriod = DayPeriod.of(now.atZone(clock.zone).toLocalTime()),
            sharingStatus = when {
                !settings.shareLocation -> SharingStatus.Off
                !permissions.status().foreground -> SharingStatus.PermissionNeeded
                else -> SharingStatus.On
            },
            distance = position.distanceKm?.let { formatDistance(it, settings.distanceUnit) },
            distanceUnit = settings.distanceUnit,
            unavailableReason = position.unavailableReason,
            freshness = position.freshness,
            partnerUpdatedAgo = position.updatedAgo,
            myCity = mine?.city,
            partnerCity = partner?.city,
            partnerDirection = position.bearingDegrees?.let(CompassDirection::fromBearing),
            bearingDegrees = position.bearingDegrees?.toFloat(),
            daysUntilReunion = reunion?.let { countdownUntil(it.meetAt, now).days },
            memoryCount = memories.size,
            newMemoryCount = memories.count { it.sender == MemorySender.Partner && it.viewedAt == null },
        )
    }

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}
