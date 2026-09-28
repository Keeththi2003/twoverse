package app.twoverse.feature.splash

import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.fake.FakeAuthRepository
import app.twoverse.core.data.fake.FakeBirthdayRepository
import app.twoverse.core.data.fake.FakeCoupleRepository
import app.twoverse.core.model.AuthState
import app.twoverse.core.model.BirthdayWelcome
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val coupleRepository = FakeCoupleRepository()
    private val birthdayRepository = FakeBirthdayRepository()
    private val clock = Clock.fixed(Instant.parse("2026-09-28T09:00:00Z"), ZoneId.of("Asia/Colombo"))

    private fun createViewModel() = SplashViewModel(authRepository, coupleRepository, birthdayRepository, clock)

    private fun welcome(showOn: LocalDate? = null, seen: Boolean = false) =
        BirthdayWelcome(message = "Happy birthday", fromName = "Her", photoUrl = null, showOn = showOn, seen = seen)

    private fun TestScope.collectState(viewModel: SplashViewModel) {
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
    }

    @Test
    fun staysLoadingUntilMinimumDisplayTimePasses() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        collectState(viewModel)

        advanceTimeBy(SplashViewModel.MinimumDisplayMillis - 1)
        runCurrent()

        assertEquals(SplashUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun waitsWhileTheSavedSessionIsRestored() = runTest(mainDispatcherRule.testDispatcher) {
        val restoring = object : AuthRepository by authRepository {
            override val authState = MutableStateFlow<AuthState>(AuthState.Loading)
        }
        val viewModel = SplashViewModel(restoring, coupleRepository, birthdayRepository, clock)
        collectState(viewModel)
        advanceTimeBy(SplashViewModel.MinimumDisplayMillis * 5)
        runCurrent()

        assertEquals(SplashUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun signedOutUserGoesToWelcome() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Welcome), viewModel.uiState.value)
    }

    @Test
    fun signedInUnpairedUserGoesToPair() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithGoogle("token", "nonce")
        val viewModel = createViewModel()
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Pair), viewModel.uiState.value)
    }

    @Test
    fun pairedUserWithUnseenBirthdayWelcomeGoesToBirthday() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithGoogle("token", "nonce")
        coupleRepository.join("AB72-KP91")
        birthdayRepository.setWelcome(welcome())
        val viewModel = createViewModel()
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Birthday), viewModel.uiState.value)
    }

    @Test
    fun pairedUserWithSeenBirthdayWelcomeGoesToHome() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithGoogle("token", "nonce")
        coupleRepository.join("AB72-KP91")
        birthdayRepository.setWelcome(welcome())
        birthdayRepository.markSeen()
        val viewModel = createViewModel()
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Home), viewModel.uiState.value)
    }

    @Test
    fun birthdayWelcomeForALaterDateWaitsForThatDay() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithGoogle("token", "nonce")
        coupleRepository.join("AB72-KP91")
        birthdayRepository.setWelcome(welcome(showOn = LocalDate.of(2026, 9, 29)))
        val viewModel = createViewModel()
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Home), viewModel.uiState.value)
    }

    @Test
    fun birthdayWelcomeShowsOnItsDay() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithGoogle("token", "nonce")
        coupleRepository.join("AB72-KP91")
        birthdayRepository.setWelcome(welcome(showOn = LocalDate.of(2026, 9, 28)))
        val viewModel = createViewModel()
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Birthday), viewModel.uiState.value)
    }
}
