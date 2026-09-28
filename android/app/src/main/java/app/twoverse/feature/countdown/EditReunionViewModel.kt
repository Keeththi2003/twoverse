package app.twoverse.feature.countdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.ReunionRepository
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.ReunionPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Set, edit or clear the shared reunion (FR-CNT-1); times are saved in UTC (FR-CNT-6). */
@HiltViewModel
class EditReunionViewModel @Inject constructor(
    private val reunionRepository: ReunionRepository,
    private val clock: Clock,
) : ViewModel() {

    private val state = MutableStateFlow(EditReunionUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<EditReunionUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val reunion = reunionRepository.reunion.first()
            val local = reunion?.meetAt?.atZone(clock.zone)
            state.update {
                it.copy(
                    isLoading = false,
                    date = local?.toLocalDate(),
                    hasTime = reunion?.hasTime ?: false,
                    time = local?.takeIf { reunion.hasTime }?.toLocalTime(),
                    place = reunion?.place.orEmpty(),
                    note = reunion?.note.orEmpty(),
                    canClear = reunion != null,
                )
            }
        }
    }

    fun onOpenPicker(picker: ReunionPicker) {
        state.update { it.copy(openPicker = picker) }
    }

    fun onDismissPicker() {
        state.update { it.copy(openPicker = null) }
    }

    fun onDateSelected(date: LocalDate) {
        state.update { it.copy(date = date, openPicker = null, problem = null) }
    }

    /** Turning the time on asks for one straight away. */
    fun onHasTimeChange(hasTime: Boolean) {
        state.update {
            it.copy(hasTime = hasTime, openPicker = if (hasTime) ReunionPicker.Time else null, problem = null)
        }
    }

    fun onTimeSelected(time: LocalTime) {
        state.update { it.copy(time = time, hasTime = true, openPicker = null, problem = null) }
    }

    fun onPlaceChange(place: String) {
        state.update { it.copy(place = place.take(ReunionPlan.MaxPlaceLength)) }
    }

    fun onNoteChange(note: String) {
        state.update { it.copy(note = note.take(ReunionPlan.MaxNoteLength)) }
    }

    fun onSave() {
        val current = state.value
        if (current.isSaving) return
        val date = current.date ?: run {
            state.update { it.copy(problem = EditReunionProblem.DateMissing) }
            return
        }
        val time = current.time.takeIf { current.hasTime }
        val meetAt = date.atTime(time ?: LocalTime.MIDNIGHT).atZone(clock.zone).toInstant()
        val inPast = if (time != null) !meetAt.isAfter(clock.instant()) else date.isBefore(LocalDate.now(clock))
        if (inPast) {
            state.update { it.copy(problem = EditReunionProblem.InPast) }
            return
        }
        val plan = ReunionPlan(
            meetAt = meetAt,
            hasTime = time != null,
            place = current.place.trim().ifEmpty { null },
            note = current.note.trim().ifEmpty { null },
        )
        finishWith { reunionRepository.saveReunion(plan) }
    }

    fun onClearRequested() {
        state.update { it.copy(isClearConfirmOpen = true) }
    }

    fun onClearDismissed() {
        state.update { it.copy(isClearConfirmOpen = false) }
    }

    fun onClearConfirmed() {
        state.update { it.copy(isClearConfirmOpen = false) }
        finishWith { reunionRepository.clearReunion() }
    }

    private fun finishWith(action: suspend () -> DataResult<Unit>) {
        state.update { it.copy(isSaving = true, error = null, problem = null) }
        viewModelScope.launch {
            when (val result = action()) {
                is DataResult.Success -> state.update { it.copy(isSaving = false, isDone = true) }
                is DataResult.Failure -> state.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }
}
