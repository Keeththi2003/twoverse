package app.twoverse.feature.orbit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.Milestone
import app.twoverse.core.common.UpcomingMilestone
import app.twoverse.core.common.dateOfDay
import app.twoverse.core.common.dayNumber
import app.twoverse.core.common.meetupStats
import app.twoverse.core.common.nextAnniversary
import app.twoverse.core.common.nextDayMilestone
import app.twoverse.core.common.nextMilestone
import app.twoverse.core.common.ticks
import app.twoverse.core.common.togetherDuration
import app.twoverse.core.data.OrbitRepository
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.Meetup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class OrbitViewModel @Inject constructor(
    private val orbitRepository: OrbitRepository,
    private val clock: Clock,
) : ViewModel() {

    private val interaction = MutableStateFlow(OrbitUiState())

    /** The local date, re-checked every minute so the counts turn over at midnight. */
    private val today = clock.ticks(DateCheckMillis).map { LocalDate.now(clock) }.distinctUntilChanged()

    val uiState: StateFlow<OrbitUiState> = combine(
        interaction,
        orbitRepository.togetherSince,
        orbitRepository.meetups,
        today,
    ) { state, since, meetups, today -> state.withOrbit(since, meetups, today) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), OrbitUiState())

    fun onDelete(id: String) {
        interaction.update { it.copy(pendingDeleteId = id, error = null) }
    }

    fun onDeleteDismissed() {
        interaction.update { it.copy(pendingDeleteId = null) }
    }

    fun onDeleteConfirmed() {
        val id = interaction.value.pendingDeleteId ?: return
        interaction.update { it.copy(pendingDeleteId = null) }
        viewModelScope.launch {
            val result = orbitRepository.deleteMeetup(id)
            interaction.update { it.copy(error = (result as? DataResult.Failure)?.error) }
        }
    }

    private fun OrbitUiState.withOrbit(since: LocalDate?, meetups: List<Meetup>, today: LocalDate): OrbitUiState {
        val duration = since?.let { togetherDuration(it, today) }
        val started = since?.takeIf { duration != null }
        val celebration = started?.let { nextMilestone(it, today) }?.takeIf { it.isToday }?.let {
            when (val milestone = it.milestone) {
                is Milestone.Years -> Celebration.Anniversary(milestone.years)
                is Milestone.Days -> Celebration.DayMilestone(milestone.day)
            }
        }
        return copy(
            isLoading = false,
            togetherSince = since,
            duration = duration,
            nextAnniversary = started?.let { nextAnniversary(it, today) },
            nextDayMilestone = started?.let { dayMilestone(it, today) },
            celebration = celebration,
            stats = meetupStats(meetups, today),
            meetups = meetups.sortedWith(compareByDescending<Meetup> { it.startDate }.thenByDescending { it.lastDay }),
        )
    }

    /** The anniversary has its own card, so this card always shows a day count. */
    private fun dayMilestone(since: LocalDate, today: LocalDate): UpcomingMilestone {
        val day = nextDayMilestone(dayNumber(since, today))
        val date = dateOfDay(since, day)
        return UpcomingMilestone(Milestone.Days(day), date, ChronoUnit.DAYS.between(today, date))
    }

    private companion object {
        const val DateCheckMillis = 60_000L
        const val StopTimeoutMillis = 5_000L
    }
}
