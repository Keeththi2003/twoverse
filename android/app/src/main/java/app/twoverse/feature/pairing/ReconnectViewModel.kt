package app.twoverse.feature.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Reconnecting after a disconnect (SRS 12): the first partner asks, the other confirms, and the
 * server restores the couple with its memories, reunion and Shooting Stars.
 */
@HiltViewModel
class ReconnectViewModel @Inject constructor(
    private val coupleRepository: CoupleRepository,
    private val clock: Clock,
) : ViewModel() {

    private val interaction = MutableStateFlow(Interaction())

    val uiState: StateFlow<ReconnectUiState> = combine(
        interaction,
        coupleRepository.couple,
        coupleRepository.endedCouple,
    ) { interaction, couple, ended ->
        when {
            couple != null -> ReconnectUiState.Reconnected
            ended == null -> ReconnectUiState.Unavailable
            else -> ReconnectUiState.Available(
                deleteOn = ended.deleteAfter.atZone(clock.zone).toLocalDate(),
                request = ended.reconnectRequest,
                isReconnecting = interaction.isReconnecting,
                error = interaction.error,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = ReconnectUiState.Loading,
    )

    /** Asks to reconnect, or confirms the partner's request. */
    fun onReconnect() {
        val current = uiState.value as? ReconnectUiState.Available ?: return
        if (!current.canReconnect) return
        interaction.update { Interaction(isReconnecting = true) }
        viewModelScope.launch {
            val result = coupleRepository.reconnect()
            interaction.update { Interaction(error = (result as? DataResult.Failure)?.error) }
        }
    }

    private data class Interaction(val isReconnecting: Boolean = false, val error: DataError? = null)

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}
