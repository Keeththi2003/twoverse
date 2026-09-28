package app.twoverse.feature.birthday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.BirthdayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BirthdayViewModel @Inject constructor(
    private val birthdayRepository: BirthdayRepository,
) : ViewModel() {

    private val entered = MutableStateFlow(false)

    val uiState: StateFlow<BirthdayUiState> = combine(birthdayRepository.welcome, entered) { welcome, entered ->
        if (welcome == null) {
            BirthdayUiState.None
        } else {
            BirthdayUiState.Welcome(
                message = welcome.message,
                fromName = welcome.fromName,
                photoUrl = welcome.photoUrl,
                isEntered = entered,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = BirthdayUiState.Loading,
    )

    /**
     * Marks the welcome as seen so it only shows once (FR-BDY-3). Continues even if that fails
     * offline; it is then shown once more next time.
     */
    fun onEnter() {
        viewModelScope.launch {
            birthdayRepository.markSeen()
            entered.value = true
        }
    }

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}
