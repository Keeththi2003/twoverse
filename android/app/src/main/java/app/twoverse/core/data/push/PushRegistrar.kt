package app.twoverse.core.data.push

import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.data.PushRepository
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.model.AuthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps what the server needs to notify this user up to date: the device's push token and the
 * time zone reunion-day notifications use. The token is removed on log out.
 */
@Singleton
class PushRegistrar @Inject constructor(
    private val authRepository: AuthRepository,
    private val pushRepository: PushRepository,
    private val profileRepository: ProfileRepository,
    private val clock: Clock,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    /** Registers on every sign-in (including after a log out, which removes the token) and at app start. */
    fun start() {
        appScope.launch {
            authRepository.authState
                .distinctUntilChanged()
                .filterIsInstance<AuthState.SignedIn>()
                .collect {
                    pushRepository.registerThisDevice()
                    profileRepository.updateTimeZone(clock.zone.id)
                }
        }
    }

    /** FCM issued a new token; saved only while someone is signed in. */
    fun onNewToken(token: String) {
        appScope.launch {
            if (authRepository.authState.first { it !is AuthState.Loading } is AuthState.SignedIn) {
                pushRepository.registerThisDevice(token)
            }
        }
    }
}
