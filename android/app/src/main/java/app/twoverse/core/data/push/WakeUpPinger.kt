package app.twoverse.core.data.push

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.PushRepository
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.model.AuthState
import app.twoverse.core.model.PartnerPush
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/** Asks the partner's phone for a fresh location each time Twoverse is opened (FR-LOC-5). */
@Singleton
class WakeUpPinger @Inject constructor(
    private val authRepository: AuthRepository,
    private val pushRepository: PushRepository,
    clock: Clock,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val throttle = PingThrottle(clock, Duration.ofMinutes(1))

    fun start() {
        appScope.launch {
            ProcessLifecycleOwner.get().lifecycle.currentStateFlow
                .map { it.isAtLeast(Lifecycle.State.STARTED) }
                .distinctUntilChanged()
                .filter { it }
                .collect { pingIfSignedIn() }
        }
    }

    private suspend fun pingIfSignedIn() {
        val signedIn = authRepository.authState.first { it !is AuthState.Loading } is AuthState.SignedIn
        if (signedIn && throttle.tryAcquire()) pushRepository.sendToPartner(PartnerPush.WakeUp)
    }
}
