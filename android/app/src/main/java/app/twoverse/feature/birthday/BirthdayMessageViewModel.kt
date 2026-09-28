package app.twoverse.feature.birthday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.BirthdayRepository
import app.twoverse.core.model.BirthdayMessageDraft
import app.twoverse.core.model.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class BirthdayMessageViewModel @Inject constructor(
    private val birthdayRepository: BirthdayRepository,
    clock: Clock,
) : ViewModel() {

    private val state = MutableStateFlow(BirthdayMessageUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<BirthdayMessageUiState> = state.asStateFlow()

    init {
        load()
    }

    /** Loads the saved welcome; also used to retry. */
    fun load() {
        state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = birthdayRepository.myMessage()) {
                is DataResult.Failure -> state.update { it.copy(isLoading = false, loadError = result.error) }
                is DataResult.Success -> state.update {
                    val saved = result.value
                    it.copy(
                        isLoading = false,
                        message = saved?.message.orEmpty(),
                        showOn = saved?.showOn,
                        savedPhotoUrl = saved?.photoUrl,
                        wasSeen = saved?.seen == true,
                    )
                }
            }
        }
    }

    fun onMessageChange(message: String) {
        state.update { it.copy(message = message.take(BirthdayMessageDraft.MaxMessageLength), error = null) }
    }

    fun onPhotoPicked(uri: String) {
        state.update { it.copy(newPhotoUri = uri, isPhotoRemoved = false, error = null) }
    }

    fun onRemovePhoto() {
        state.update { it.copy(newPhotoUri = null, isPhotoRemoved = true) }
    }

    fun onOpenDatePicker() {
        state.update { it.copy(isDatePickerOpen = true) }
    }

    fun onDismissDatePicker() {
        state.update { it.copy(isDatePickerOpen = false) }
    }

    fun onDateSelected(date: LocalDate) {
        state.update { current ->
            current.copy(showOn = date.takeUnless { it.isBefore(current.today) } ?: current.showOn, isDatePickerOpen = false)
        }
    }

    /** Shows it the next time the partner opens the app instead of on a date. */
    fun onClearDate() {
        state.update { it.copy(showOn = null) }
    }

    fun onSave() {
        val current = state.value
        if (!current.canSave) return
        state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = birthdayRepository.saveMyMessage(current.toDraft())) {
                is DataResult.Success -> state.update { it.copy(isSaving = false, isSaved = true) }
                is DataResult.Failure -> state.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }
}
