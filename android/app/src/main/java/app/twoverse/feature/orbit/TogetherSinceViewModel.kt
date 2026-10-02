package app.twoverse.feature.orbit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.OrbitRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
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
 * Key of the flag in the type-safe `TogetherSinceRoute(afterPairing)` destination; navigation stores
 * route properties in the SavedStateHandle under their names.
 */
internal const val AfterPairingKey = "afterPairing"

/** Sets or changes when the relationship began (FR-ORB-1, FR-ORB-2). */
@HiltViewModel
class TogetherSinceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orbitRepository: OrbitRepository,
    private val clock: Clock,
) : ViewModel() {

    private val state = MutableStateFlow(
        TogetherSinceUiState(isAfterPairing = savedStateHandle[AfterPairingKey] ?: false, today = LocalDate.now(clock)),
    )
    val uiState: StateFlow<TogetherSinceUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = orbitRepository.togetherSince.first()
            state.update { it.copy(isLoading = false, date = saved) }
        }
    }

    fun onOpenPicker() {
        state.update { it.copy(isPickerOpen = true) }
    }

    fun onDismissPicker() {
        state.update { it.copy(isPickerOpen = false) }
    }

    fun onDateSelected(date: LocalDate) {
        state.update {
            if (date.isAfter(it.today)) {
                it.copy(isPickerOpen = false, error = DataError.DateInFuture)
            } else {
                it.copy(date = date, isPickerOpen = false, error = null)
            }
        }
    }

    fun onSave() {
        val current = state.value
        val date = current.date ?: return
        if (!current.canSave) return
        state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = orbitRepository.setTogetherSince(date)) {
                is DataResult.Success -> state.update { it.copy(isSaving = false, isDone = true) }
                is DataResult.Failure -> state.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }

    /** Only after pairing; Our Universe then offers to set it later (FR-ORB-2). */
    fun onSkip() {
        state.update { it.copy(isDone = true) }
    }
}
