package app.twoverse.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.BirthdayRepository
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.model.BirthdayWelcome
import app.twoverse.core.model.Couple
import app.twoverse.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    authRepository: AuthRepository,
    coupleRepository: CoupleRepository,
    birthdayRepository: BirthdayRepository,
) : ViewModel() {

    private val minimumDisplay = flow {
        delay(MinimumDisplayMillis)
        emit(Unit)
    }

    val uiState: StateFlow<SplashUiState> = combine(
        authRepository.currentUser,
        coupleRepository.couple,
        birthdayRepository.welcome,
        minimumDisplay,
    ) { user, couple, welcome, _ -> destinationFor(user, couple, welcome) }
        .take(1)
        .map<SplashDestination, SplashUiState> { SplashUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
            initialValue = SplashUiState.Loading,
        )

    private fun destinationFor(
        user: UserProfile?,
        couple: Couple?,
        welcome: BirthdayWelcome?,
    ): SplashDestination = when {
        user == null -> SplashDestination.Welcome
        couple == null -> SplashDestination.Pair
        welcome != null && !welcome.seen -> SplashDestination.Birthday
        else -> SplashDestination.Home
    }

    companion object {
        /** Keeps the splash visible long enough to read, within the 2 s cold-start target (NFR-PRF-1). */
        const val MinimumDisplayMillis = 1_200L
        private const val StopTimeoutMillis = 5_000L
    }
}
