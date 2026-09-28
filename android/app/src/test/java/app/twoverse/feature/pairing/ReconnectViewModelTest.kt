package app.twoverse.feature.pairing

import app.twoverse.core.data.fake.FakeCoupleRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.EndedCouple
import app.twoverse.core.model.ReconnectRequest
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ReconnectViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val coupleRepository = FakeCoupleRepository()
    private val clock = Clock.fixed(Instant.parse("2026-09-28T09:00:00Z"), ZoneId.of("Asia/Colombo"))

    private fun ended(request: ReconnectRequest) = EndedCouple(
        id = "couple",
        endedAt = Instant.parse("2026-09-27T09:00:00Z"),
        deleteAfter = Instant.parse("2026-10-04T20:00:00Z"),
        reconnectRequest = request,
    )

    private fun TestScope.createViewModel(): ReconnectViewModel {
        val viewModel = ReconnectViewModel(coupleRepository, clock)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    @Test
    fun nothingToReconnectAfterTheGracePeriod() = runTest(mainDispatcherRule.testDispatcher) {
        assertEquals(ReconnectUiState.Unavailable, createViewModel().uiState.value)
    }

    @Test
    fun showsTheLastDayInLocalTime() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.setEndedCouple(ended(ReconnectRequest.None))

        val state = createViewModel().uiState.value as ReconnectUiState.Available

        assertEquals(LocalDate.of(2026, 10, 5), state.deleteOn)
    }

    @Test
    fun askingFirstWaitsForThePartner() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.setEndedCouple(ended(ReconnectRequest.None))
        val viewModel = createViewModel()

        viewModel.onReconnect()
        runCurrent()

        val state = viewModel.uiState.value as ReconnectUiState.Available
        assertEquals(ReconnectRequest.ByMe, state.request)
        assertFalse(state.canReconnect)
    }

    @Test
    fun confirmingThePartnersRequestReconnects() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.setEndedCouple(ended(ReconnectRequest.ByPartner))
        val viewModel = createViewModel()

        viewModel.onReconnect()
        runCurrent()

        assertEquals(ReconnectUiState.Reconnected, viewModel.uiState.value)
    }

    @Test
    fun aFailedRequestShowsTheError() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.setEndedCouple(ended(ReconnectRequest.None))
        val viewModel = createViewModel()
        coupleRepository.failNextWith = DataError.Network

        viewModel.onReconnect()
        runCurrent()

        val state = viewModel.uiState.value as ReconnectUiState.Available
        assertEquals(DataError.Network, state.error)
        assertEquals(ReconnectRequest.None, state.request)
    }

    @Test
    fun disconnectingStartsTheGracePeriod() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.join("AB12-CD34")
        coupleRepository.disconnect()

        assertEquals(ReconnectRequest.None, (createViewModel().uiState.value as ReconnectUiState.Available).request)
    }
}
