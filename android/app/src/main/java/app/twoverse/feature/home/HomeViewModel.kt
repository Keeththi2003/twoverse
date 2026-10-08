package app.twoverse.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.DayPeriod
import app.twoverse.core.common.countdownUntil
import app.twoverse.core.common.formatDistance
import app.twoverse.core.common.ReunionPhase
import app.twoverse.core.common.partnerPosition
import app.twoverse.core.common.reunionPhase
import app.twoverse.core.common.togetherDuration
import app.twoverse.core.common.ticks
import app.twoverse.core.data.LocationPermissionChecker
import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.MemoryRepository
import app.twoverse.core.data.OrbitRepository
import app.twoverse.core.data.ReunionRepository
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.local.UserPreferences
import app.twoverse.core.model.Meetup
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender
import app.twoverse.core.model.Reunion
import app.twoverse.core.model.UserLocation
import app.twoverse.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import kotlin.math.roundToInt
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    settingsRepository: SettingsRepository,
    private val reunionRepository: ReunionRepository,
    memoryRepository: MemoryRepository,
    orbitRepository: OrbitRepository,
    private val permissions: LocationPermissionChecker,
    private val preferences: UserPreferences,
    private val clock: Clock,
) : ViewModel() {

    /** True until the notification permission has been asked for once (Android 13+). */
    val shouldAskNotificationPermission: StateFlow<Boolean> = preferences.notificationPermissionAsked
        .map { asked -> !asked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), false)

    fun onNotificationPermissionAsked() {
        viewModelScope.launch { preferences.setNotificationPermissionAsked() }
    }

    private val locations = combine(
        locationRepository.myLocation,
        locationRepository.partnerLocation,
    ) { mine, partner -> mine to partner }

    private val orbit = combine(
        orbitRepository.togetherSince,
        orbitRepository.meetups,
        preferences.dismissedMeetupQuestion,
    ) { since, meetups, dismissed -> OrbitInputs(since, meetups, dismissed) }

    val uiState: StateFlow<HomeUiState> = combine(
        locations,
        settingsRepository.settings,
        reunionRepository.reunion,
        memoryRepository.memories,
        combine(clock.ticks(), orbit, ::Pair),
    ) { (mine, partner), settings, reunion, memories, (now, orbit) ->
        buildState(mine, partner, settings, reunion, memories, now).withOrbit(orbit, reunion, now)
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
            partnerBearing = position.bearingDegrees?.let { (it.roundToInt() % FullCircle + FullCircle) % FullCircle },
            daysUntilReunion = reunion?.let { countdownUntil(it.meetAt, now).days },
            memoryCount = memories.size,
            newMemoryCount = memories.count { it.sender == MemorySender.Partner && it.viewedAt == null },
        )
    }

    /** No: this device won't ask about this reunion again (FR-ORB-10). Yes opens the meetup editor. */
    fun onMeetupQuestionDismissed() {
        viewModelScope.launch {
            reunionRepository.reunion.first()?.let { preferences.setDismissedMeetupQuestion(it.meetAt) }
        }
    }

    private fun HomeUiState.Success.withOrbit(orbit: OrbitInputs, reunion: Reunion?, now: Instant): HomeUiState.Success {
        val today = now.atZone(clock.zone).toLocalDate()
        val duration = orbit.since?.let { togetherDuration(it, today) }
        val question = reunion?.takeIf { passed ->
            reunionPhase(passed.meetAt, now, clock.zone) == ReunionPhase.Past &&
                orbit.meetups.none { it.fromReunionAt == passed.meetAt } &&
                orbit.dismissed != passed.meetAt
        }
        return copy(
            orbit = duration?.let { HomeOrbit(totalDays = it.totalDays, timesMet = orbit.meetups.size) },
            askTogetherSince = orbit.since == null,
            meetupQuestion = question?.meetAt?.atZone(clock.zone)?.toLocalDate(),
        )
    }

    private data class OrbitInputs(val since: LocalDate?, val meetups: List<Meetup>, val dismissed: Instant?)

    private companion object {
        const val FullCircle = 360
        const val StopTimeoutMillis = 5_000L
    }
}
