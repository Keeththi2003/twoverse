package app.twoverse.feature.orbit

import androidx.lifecycle.SavedStateHandle
import app.twoverse.core.data.fake.FakeOrbitRepository
import app.twoverse.core.model.DataError
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TogetherSinceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeOrbitRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-12T09:00:00Z"), ZoneId.of("Asia/Colombo"))
    private val today = LocalDate.of(2026, 10, 12)

    private fun TestScope.createViewModel(afterPairing: Boolean = false): TogetherSinceViewModel {
        val viewModel = TogetherSinceViewModel(SavedStateHandle(mapOf(AfterPairingKey to afterPairing)), repository, clock)
        runCurrent()
        return viewModel
    }

    @Test
    fun startsWithTheSavedDate() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setTogetherSince(LocalDate.of(2024, 2, 14))

        val state = createViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals(LocalDate.of(2024, 2, 14), state.date)
        assertEquals(today, state.today)
    }

    @Test
    fun savesTheChosenDate() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onOpenPicker()
        viewModel.onDateSelected(LocalDate.of(2024, 2, 14))
        assertFalse(viewModel.uiState.value.isPickerOpen)
        viewModel.onSave()
        runCurrent()

        assertTrue(viewModel.uiState.value.isDone)
        assertEquals(LocalDate.of(2024, 2, 14), repository.togetherSince.first())
    }

    @Test
    fun aFutureDateIsRefused() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onDateSelected(today.plusDays(1))

        assertNull(viewModel.uiState.value.date)
        assertEquals(DataError.DateInFuture, viewModel.uiState.value.error)
    }

    @Test
    fun todayIsAllowed() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onDateSelected(today)

        assertEquals(today, viewModel.uiState.value.date)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun afterPairingItCanBeSkipped() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(afterPairing = true)
        assertTrue(viewModel.uiState.value.isAfterPairing)

        viewModel.onSkip()

        assertTrue(viewModel.uiState.value.isDone)
        assertNull(repository.togetherSince.first())
    }

    @Test
    fun aFailedSaveKeepsTheDateForARetry() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onDateSelected(LocalDate.of(2024, 2, 14))
        repository.failNextWith(DataError.Network)

        viewModel.onSave()
        runCurrent()

        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isDone)
        assertEquals(LocalDate.of(2024, 2, 14), viewModel.uiState.value.date)
    }
}
