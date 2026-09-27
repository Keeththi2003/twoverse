package app.twoverse.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.BirthdayRepository
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.model.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import javax.inject.Inject

/** Chooses the first screen from the saved session and couple status (FR-ONB-3). */
@HiltViewModel
class SplashViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val coupleRepository: CoupleRepository,
    private val birthdayRepository: BirthdayRepository,
) : ViewModel() {

    private val minimumDisplay = flow {
        delay(MinimumDisplayMillis)
        emit(Unit)
    }

    private val destination = authRepository.authState
        .filterNot { it is AuthState.Loading }
        .map { state -> if (state is AuthState.SignedIn) signedInDestination() else SplashDestination.Welcome }

    val uiState: StateFlow<SplashUiState> = combine(destination, minimumDisplay) { destination, _ -> destination }
        .take(1)
        .map<SplashDestination, SplashUiState> { SplashUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
            initialValue = SplashUiState.Loading,
        )

    private suspend fun signedInDestination(): SplashDestination {
        if (coupleRepository.couple.first() == null) return SplashDestination.Pair
        val welcome = birthdayRepository.welcome.first()
        return if (welcome != null && !welcome.seen) SplashDestination.Birthday else SplashDestination.Home
    }

    companion object {
        /** Keeps the splash visible long enough to read, within the 2 s cold-start target (NFR-PRF-1). */
        const val MinimumDisplayMillis = 1_200L
        private const val StopTimeoutMillis = 5_000L
    }
}
