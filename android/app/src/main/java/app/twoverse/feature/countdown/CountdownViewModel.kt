package app.twoverse.feature.countdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.ReunionPhase
import app.twoverse.core.common.countdownProgress
import app.twoverse.core.common.countdownUntil
import app.twoverse.core.common.reunionPhase
import app.twoverse.core.common.ticks
import app.twoverse.core.data.ReunionRepository
import app.twoverse.core.model.Reunion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** The shared countdown, ticking every second in the user's local time (FR-CNT-2, FR-CNT-3, FR-CNT-6). */
@HiltViewModel
class CountdownViewModel @Inject constructor(
    reunionRepository: ReunionRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<CountdownUiState> = combine(reunionRepository.reunion, clock.ticks()) { reunion, now ->
        CountdownUiState(content = contentFor(reunion, now))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = CountdownUiState(),
    )

    private fun contentFor(reunion: Reunion?, now: Instant): CountdownContent {
        if (reunion == null) return CountdownContent.NoDate
        return when (reunionPhase(reunion.meetAt, now, clock.zone)) {
            ReunionPhase.Past -> CountdownContent.AfterReunion
            ReunionPhase.Today -> CountdownContent.Celebrating(details(reunion))
            ReunionPhase.Upcoming -> CountdownContent.Counting(
                timeLeft = countdownUntil(reunion.meetAt, now),
                progress = countdownProgress(reunion.dateSetAt, reunion.meetAt, now),
                plan = details(reunion),
            )
        }
    }

    private fun details(reunion: Reunion): ReunionDetails {
        val local = reunion.meetAt.atZone(clock.zone)
        return ReunionDetails(
            date = local.toLocalDate(),
            time = if (reunion.hasTime) local.toLocalTime() else null,
            place = reunion.place,
            note = reunion.note,
        )
    }

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}
