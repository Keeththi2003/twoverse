package app.twoverse.feature.orbit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.OrbitRepository
import app.twoverse.core.data.ReunionRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.MeetupDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Keys of the type-safe `MeetupEditorRoute(meetupId, fromReunion)` destination; navigation stores
 * route properties in the SavedStateHandle under their names.
 */
internal const val MeetupIdKey = "meetupId"
internal const val FromReunionKey = "fromReunion"

/**
 * Adds a meetup, edits one ([MeetupIdKey]), or records the passed reunion ([FromReunionKey]) with
 * its date and place filled in and an editable end date (FR-ORB-3, FR-ORB-10).
 */
@HiltViewModel
class MeetupEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orbitRepository: OrbitRepository,
    private val reunionRepository: ReunionRepository,
    private val clock: Clock,
) : ViewModel() {

    private val meetupId: String? = savedStateHandle[MeetupIdKey]
    private val fromReunion: Boolean = savedStateHandle[FromReunionKey] ?: false
    private val state = MutableStateFlow(MeetupEditorUiState(isEditing = meetupId != null, today = LocalDate.now(clock)))
    val uiState: StateFlow<MeetupEditorUiState> = state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        when {
            meetupId != null -> {
                val meetup = orbitRepository.meetups.first().firstOrNull { it.id == meetupId }
                state.update {
                    if (meetup == null) {
                        it.copy(isLoading = false, loadError = DataError.Unknown)
                    } else {
                        it.copy(
                            isLoading = false,
                            startDate = meetup.startDate,
                            severalDays = meetup.endDate != null,
                            endDate = meetup.endDate,
                            place = meetup.place.orEmpty(),
                            note = meetup.note.orEmpty(),
                        )
                    }
                }
            }
            fromReunion -> {
                val reunion = reunionRepository.reunion.first()
                state.update {
                    it.copy(
                        isLoading = false,
                        startDate = reunion?.meetAt?.atZone(clock.zone)?.toLocalDate(),
                        place = reunion?.place.orEmpty().take(MeetupDraft.MaxPlaceLength),
                        fromReunionAt = reunion?.meetAt,
                    )
                }
            }
            else -> state.update { it.copy(isLoading = false) }
        }
    }

    fun onOpenPicker(picker: MeetupPicker) {
        state.update { it.copy(openPicker = picker) }
    }

    fun onDismissPicker() {
        state.update { it.copy(openPicker = null) }
    }

    fun onStartDateSelected(date: LocalDate) {
        state.update { it.copy(startDate = date, openPicker = null, problem = null) }
    }

    fun onEndDateSelected(date: LocalDate) {
        state.update { it.copy(endDate = date, severalDays = true, openPicker = null, problem = null) }
    }

    /** Turning several days on asks for the last day straight away. */
    fun onSeveralDaysChange(severalDays: Boolean) {
        state.update {
            it.copy(severalDays = severalDays, openPicker = if (severalDays) MeetupPicker.End else null, problem = null)
        }
    }

    fun onPlaceChange(place: String) {
        state.update { it.copy(place = place.take(MeetupDraft.MaxPlaceLength)) }
    }

    fun onNoteChange(note: String) {
        state.update { it.copy(note = note.take(MeetupDraft.MaxNoteLength)) }
    }

    fun onSave() {
        val current = state.value
        if (!current.canSave) return
        val end = current.endDate.takeIf { current.severalDays }
        meetupProblem(current.startDate, end, current.today)?.let { problem ->
            state.update { it.copy(problem = problem) }
            return
        }
        val start = current.startDate ?: return
        state.update { it.copy(isSaving = true, error = null, problem = null) }
        viewModelScope.launch {
            when (val result = orbitRepository.saveMeetup(meetupId, current.toDraft(start))) {
                is DataResult.Success -> state.update { it.copy(isSaving = false, isSaved = true) }
                is DataResult.Failure -> state.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }
}
