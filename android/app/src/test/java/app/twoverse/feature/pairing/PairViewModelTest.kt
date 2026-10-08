package app.twoverse.feature.pairing

import app.twoverse.core.data.fake.FakeAuthRepository
import app.twoverse.core.data.fake.FakeCoupleRepository
import app.twoverse.core.data.fake.FakePushRepository
import app.twoverse.core.data.fake.FakeShootingStarRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.AuthState
import app.twoverse.core.model.DataError
import app.twoverse.core.model.EndedCouple
import app.twoverse.core.model.ReconnectRequest
import app.twoverse.testing.MainDispatcherRule
import app.twoverse.testing.testStar
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
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class PairViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val coupleRepository = FakeCoupleRepository()
    private val pushRepository = FakePushRepository()
    private val authRepository = FakeAuthRepository()
    private val starRepository = FakeShootingStarRepository()
    private val clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)

    private fun TestScope.createViewModel(): PairViewModel {
        val viewModel = PairViewModel(coupleRepository, pushRepository, authRepository, starRepository, clock)
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
        starRepository.setReceived(listOf(testStar()))
        val viewModel = createViewModel()

        viewModel.onPartnerCodeChange("ab12-cd34!")
        viewModel.onConnect()
        runCurrent()

        assertEquals("AB12-CD34", viewModel.uiState.value.partnerCode)
        assertTrue(viewModel.uiState.value.isConnected)
        assertTrue(viewModel.uiState.value.hasWaitingStar)
        assertEquals(listOf("send:PartnerJoined"), pushRepository.calls)
    }

    @Test
    fun afterConnectingTheStoryDateIsAskedUnlessThePartnerSetIt() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        coupleRepository.simulatePartnerJoined()
        runCurrent()
        assertTrue(viewModel.uiState.value.askTogetherSince)

        coupleRepository.setCouple(SampleData.couple.copy(togetherSince = LocalDate.of(2024, 2, 14)))
        runCurrent()
        assertFalse(viewModel.uiState.value.askTogetherSince)
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
        assertTrue(pushRepository.calls.isEmpty())

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
    fun seenShootingStarsAreNotShownAgain() = runTest(mainDispatcherRule.testDispatcher) {
        starRepository.setReceived(listOf(testStar(seenAt = clock.instant())))
        val viewModel = createViewModel()

        coupleRepository.simulatePartnerJoined()
        runCurrent()

        assertFalse(viewModel.uiState.value.hasWaitingStar)
    }

    @Test
    fun aDisconnectedCoupleCanBeReconnectedUntilItIsDeleted() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.setEndedCouple(
            EndedCouple(
                id = "couple",
                endedAt = Instant.parse("2026-09-27T09:00:00Z"),
                deleteAfter = Instant.parse("2026-10-04T09:00:00Z"),
                reconnectRequest = ReconnectRequest.ByPartner,
            ),
        )

        val reconnect = createViewModel().uiState.value.reconnect

        assertEquals(PairReconnect(deleteOn = LocalDate.of(2026, 10, 4), partnerAsked = true), reconnect)
    }

    @Test
    fun withoutADisconnectedCoupleThereIsNothingToReconnect() = runTest(mainDispatcherRule.testDispatcher) {
        assertNull(createViewModel().uiState.value.reconnect)
    }

    @Test
    fun loggingOutAfterConfirmingRemovesThePushTokenFirst() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithEmail("you@example.com", "secret1")
        val viewModel = createViewModel()

        viewModel.onLogOut()
        runCurrent()
        assertTrue(viewModel.uiState.value.isLogOutDialogOpen)
        viewModel.onLogOutConfirmed()
        runCurrent()

        assertTrue(viewModel.uiState.value.isSignedOut)
        assertEquals(listOf("unregister"), pushRepository.calls)
        assertEquals(AuthState.SignedOut, authRepository.authState.value)
    }

    @Test
    fun dismissingLogOutKeepsTheUserSignedIn() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithEmail("you@example.com", "secret1")
        val viewModel = createViewModel()

        viewModel.onLogOut()
        viewModel.onLogOutDismissed()
        runCurrent()

        assertFalse(viewModel.uiState.value.isLogOutDialogOpen)
        assertFalse(viewModel.uiState.value.isSignedOut)
    }
}
