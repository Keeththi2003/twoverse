package app.twoverse.feature.pairing

import app.twoverse.core.data.fake.FakeBirthdayRepository
import app.twoverse.core.data.fake.FakeCoupleRepository
import app.twoverse.core.model.DataError
import app.twoverse.testing.InMemoryUserPreferences
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class PairViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val coupleRepository = FakeCoupleRepository()
    private val preferences = InMemoryUserPreferences()
    private val clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)

    private fun TestScope.createViewModel(): PairViewModel {
        val viewModel = PairViewModel(coupleRepository, FakeBirthdayRepository(preferences), clock)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    @Test
    fun showsANewCodeWhileWaiting() = runTest(mainDispatcherRule.testDispatcher) {
        val state = createViewModel().uiState.value

        assertEquals("AB72-KP91", state.coupleCode)
        assertEquals(24L, state.codeExpiresInHours)
        assertTrue(state.isWaitingForPartner)
        assertFalse(state.isConnected)
    }

    @Test
    fun codeErrorCanBeRetried() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.failNextWith = DataError.Network
        val viewModel = createViewModel()
        assertEquals(DataError.Network, viewModel.uiState.value.codeError)

        viewModel.loadCode()
        runCurrent()

        assertNull(viewModel.uiState.value.codeError)
        assertEquals("AB72-KP91", viewModel.uiState.value.coupleCode)
    }

    @Test
    fun partnerJoiningThisCodeConnectsLive() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        coupleRepository.simulatePartnerJoined()
        runCurrent()

        assertTrue(viewModel.uiState.value.isConnected)
        assertFalse(viewModel.uiState.value.isWaitingForPartner)
    }

    @Test
    fun joiningAPartnersCodeConnects() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onPartnerCodeChange("ab12-cd34!")
        viewModel.onConnect()
        runCurrent()

        assertEquals("AB12-CD34", viewModel.uiState.value.partnerCode)
        assertTrue(viewModel.uiState.value.isConnected)
        assertTrue(viewModel.uiState.value.hasBirthdayWelcome)
    }

    @Test
    fun invalidCodeShowsTheSrsMessageError() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        coupleRepository.failNextWith = DataError.InvalidCoupleCode
        viewModel.onPartnerCodeChange("ZZZZ-ZZZZ")
        viewModel.onConnect()
        runCurrent()

        assertEquals(DataError.InvalidCoupleCode, viewModel.uiState.value.joinError)
        assertFalse(viewModel.uiState.value.isConnected)

        viewModel.onPartnerCodeChange("ZZZZ-ZZZY")
        runCurrent()
        assertNull(viewModel.uiState.value.joinError)
    }

    @Test
    fun alreadyPairedIsReported() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        coupleRepository.failNextWith = DataError.AlreadyPaired
        viewModel.onPartnerCodeChange("AB12-CD34")
        viewModel.onConnect()
        runCurrent()

        assertEquals(DataError.AlreadyPaired, viewModel.uiState.value.joinError)
    }

    @Test
    fun seenBirthdayWelcomeIsNotShownAgain() = runTest(mainDispatcherRule.testDispatcher) {
        preferences.setBirthdayWelcomeSeen()
        val viewModel = createViewModel()

        coupleRepository.simulatePartnerJoined()
        runCurrent()

        assertFalse(viewModel.uiState.value.hasBirthdayWelcome)
    }
}
