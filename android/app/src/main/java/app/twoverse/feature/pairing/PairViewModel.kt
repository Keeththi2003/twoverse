package app.twoverse.feature.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.ticks
import app.twoverse.core.data.BirthdayRepository
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.PushRepository
import app.twoverse.core.model.CoupleCode
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.PartnerPush
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
    private val pushRepository: PushRepository,
    birthdayRepository: BirthdayRepository,
    clock: Clock,
) : ViewModel() {

    private val coupleCode = MutableStateFlow<CoupleCode?>(null)
    private val form = MutableStateFlow(PairUiState())

    val uiState: StateFlow<PairUiState> = combine(
        form,
        coupleCode,
        coupleRepository.couple,
        birthdayRepository.welcome,
        clock.ticks(ExpiryRefreshMillis),
    ) { form, code, couple, welcome, now ->
        form.copy(
            coupleCode = code?.code,
            codeExpiresInHours = code?.let { hoursUntil(it.expiresAt, now) } ?: 0,
            isWaitingForPartner = couple == null,
            isConnected = couple != null,
            hasBirthdayWelcome = welcome != null && !welcome.seen,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = PairUiState(),
    )

    init {
        loadCode()
    }

    /** Creates this user's code (FR-PAIR-1); also used to retry after an error. */
    fun loadCode() {
        form.update { it.copy(codeError = null) }
        viewModelScope.launch {
            when (val result = coupleRepository.createCode()) {
                is DataResult.Success -> coupleCode.value = result.value
                is DataResult.Failure -> form.update { it.copy(codeError = result.error) }
            }
        }
    }

    fun onPartnerCodeChange(input: String) {
        val code = input.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(CodeLength)
        form.update { it.copy(partnerCode = code, joinError = null) }
    }

    /** Joins the partner's code (FR-PAIR-3); the couple stream then reports the connection. */
    fun onConnect() {
        val current = form.value
        if (!current.canConnect) return
        form.update { it.copy(isConnecting = true, joinError = null) }
        viewModelScope.launch {
            val result = coupleRepository.join(current.partnerCode)
            // Tells the partner who shared the code that they are connected (FR-NOT-2).
            if (result is DataResult.Success) pushRepository.sendToPartner(PartnerPush.PartnerJoined)
            form.update {
                it.copy(isConnecting = false, joinError = (result as? DataResult.Failure)?.error)
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
