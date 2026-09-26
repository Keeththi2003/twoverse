package app.twoverse.feature.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.ticks
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.model.CoupleCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class PairViewModel @Inject constructor(
    private val coupleRepository: CoupleRepository,
    clock: Clock,
) : ViewModel() {

    private val coupleCode = MutableStateFlow<CoupleCode?>(null)
    private val form = MutableStateFlow(PairUiState())

    val uiState: StateFlow<PairUiState> = combine(
        form,
        coupleCode,
        coupleRepository.couple,
        clock.ticks(ExpiryRefreshMillis),
    ) { form, code, couple, now ->
        form.copy(
            coupleCode = code?.code,
            codeExpiresInHours = code?.let { hoursUntil(it.expiresAt, now) } ?: 0,
            isWaitingForPartner = couple == null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = PairUiState(),
    )

    init {
        viewModelScope.launch { coupleCode.value = coupleRepository.generateCode() }
    }

    fun onPartnerCodeChange(input: String) {
        val code = input.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(CodeLength)
        form.update { it.copy(partnerCode = code, isInvalidCode = false) }
    }

    fun onConnect() {
        val current = form.value
        if (!current.canConnect) return
        form.update { it.copy(isConnecting = true, isInvalidCode = false) }
        viewModelScope.launch {
            val result = coupleRepository.joinWithCode(current.partnerCode)
            form.update {
                it.copy(
                    isConnecting = false,
                    isConnected = result.isSuccess,
                    isInvalidCode = result.isFailure,
                )
            }
        }
    }

    private fun hoursUntil(expiresAt: Instant, now: Instant): Long {
        val left = Duration.between(now, expiresAt)
        if (left <= Duration.ZERO) return 0
        return (left.toMinutes() + MinutesPerHour - 1) / MinutesPerHour
    }

    private companion object {
        const val CodeLength = 9
        const val MinutesPerHour = 60L
        const val ExpiryRefreshMillis = 60_000L
        const val StopTimeoutMillis = 5_000L
    }
}
