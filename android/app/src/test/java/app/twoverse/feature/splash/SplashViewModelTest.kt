package app.twoverse.feature.splash

import app.twoverse.core.data.fake.FakeAuthRepository
import app.twoverse.core.data.fake.FakeBirthdayRepository
import app.twoverse.core.data.fake.FakeCoupleRepository
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val coupleRepository = FakeCoupleRepository()
    private val birthdayRepository = FakeBirthdayRepository()

    private fun createViewModel() = SplashViewModel(authRepository, coupleRepository, birthdayRepository)

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
    fun signedOutUserGoesToWelcome() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Welcome), viewModel.uiState.value)
    }

    @Test
    fun signedInUnpairedUserGoesToPair() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithGoogle()
        val viewModel = createViewModel()
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Pair), viewModel.uiState.value)
    }

    @Test
    fun pairedUserWithUnseenBirthdayWelcomeGoesToBirthday() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithGoogle()
        coupleRepository.joinWithCode("AB72-KP91")
        val viewModel = createViewModel()
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Birthday), viewModel.uiState.value)
    }

    @Test
    fun pairedUserWithSeenBirthdayWelcomeGoesToHome() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithGoogle()
        coupleRepository.joinWithCode("AB72-KP91")
        birthdayRepository.markSeen()
        val viewModel = createViewModel()
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Home), viewModel.uiState.value)
    }
}
