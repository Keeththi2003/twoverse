package app.twoverse.feature.countdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.countdownProgress
import app.twoverse.core.common.countdownUntil
import app.twoverse.core.common.ticks
import app.twoverse.core.data.ReunionRepository
import app.twoverse.core.model.Reunion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class CountdownViewModel @Inject constructor(
    private val reunionRepository: ReunionRepository,
    private val clock: Clock,
) : ViewModel() {

    private val isDatePickerOpen = MutableStateFlow(false)

    val uiState: StateFlow<CountdownUiState> = combine(
        reunionRepository.reunion,
        clock.ticks(),
        isDatePickerOpen,
    ) { reunion, now, pickerOpen ->
        CountdownUiState(
            content = contentFor(reunion, now),
            isDatePickerOpen = pickerOpen,
            today = now.atZone(clock.zone).toLocalDate(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = CountdownUiState(),
    )

    fun onChangeDate() {
        isDatePickerOpen.value = true
    }

    fun onDismissDatePicker() {
        isDatePickerOpen.value = false
    }

    /** Moves the reunion to [date], keeping its local time, place and note (FR-CNT-1). */
    fun onDateSelected(date: LocalDate) {
        isDatePickerOpen.value = false
        viewModelScope.launch {
            val current = reunionRepository.reunion.first()
            val time = current?.takeIf { it.hasTime }?.meetAt?.atZone(clock.zone)?.toLocalTime() ?: LocalTime.MIDNIGHT
            reunionRepository.setReunion(
                Reunion(
                    meetAt = date.atTime(time).atZone(clock.zone).toInstant(),
                    hasTime = current?.hasTime ?: false,
                    place = current?.place,
                    note = current?.note,
                    updatedAt = clock.instant(),
                ),
            )
        }
    }

    private fun contentFor(reunion: Reunion?, now: Instant): CountdownContent {
        if (reunion == null) return CountdownContent.NoDate(afterReunion = false)
        val timeLeft = countdownUntil(reunion.meetAt, now)
        if (timeLeft.isReached) return CountdownContent.NoDate(afterReunion = true)
        val local = reunion.meetAt.atZone(clock.zone)
        return CountdownContent.Counting(
            timeLeft = timeLeft,
            progress = countdownProgress(reunion.updatedAt, reunion.meetAt, now),
            date = local.toLocalDate(),
            time = if (reunion.hasTime) local.toLocalTime() else null,
            place = reunion.place,
        )
    }

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}
