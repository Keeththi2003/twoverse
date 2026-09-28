package app.twoverse.feature.countdown

import app.twoverse.core.data.fake.FakeReunionRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.ReunionPlan
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class EditReunionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val colombo = ZoneId.of("Asia/Colombo")

    /** 26 September 2026, 12:00 in Colombo. */
    private val clock = Clock.fixed(Instant.parse("2026-09-26T06:30:00Z"), colombo)
    private val repository = FakeReunionRepository()

    private fun create() = EditReunionViewModel(repository, clock)

    @Test
    fun startsWithTheCurrentPlanInLocalTime() = runTest(mainDispatcherRule.testDispatcher) {
        repository.saveReunion(ReunionPlan(Instant.parse("2026-10-10T04:30:00Z"), hasTime = true, place = "Kandy", note = "Hi"))
        val viewModel = create()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(LocalDate.of(2026, 10, 10), state.date)
        assertEquals(LocalTime.of(10, 0), state.time)
        assertEquals("Kandy", state.place)
        assertTrue(state.canClear)
    }

    @Test
    fun savesTheLocalTimeAsUtc() = runTest(mainDispatcherRule.testDispatcher) {
        repository.clearReunion()
        val viewModel = create()
        runCurrent()

        viewModel.onDateSelected(LocalDate.of(2026, 10, 17))
        viewModel.onTimeSelected(LocalTime.of(18, 30))
        viewModel.onPlaceChange("  Galle  ")
        viewModel.onSave()
        runCurrent()

        val saved = repository.reunion.value
        assertEquals(Instant.parse("2026-10-17T13:00:00Z"), saved?.meetAt)
        assertEquals(true, saved?.hasTime)
        assertEquals("Galle", saved?.place)
        assertNull(saved?.note)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun aDateWithoutTimeStartsAtLocalMidnight() = runTest(mainDispatcherRule.testDispatcher) {
        repository.clearReunion()
        val viewModel = create()
        runCurrent()

        viewModel.onDateSelected(LocalDate.of(2026, 10, 17))
        viewModel.onSave()
        runCurrent()

        assertEquals(Instant.parse("2026-10-16T18:30:00Z"), repository.reunion.value?.meetAt)
        assertEquals(false, repository.reunion.value?.hasTime)
    }

    @Test
    fun aDateIsRequired() = runTest(mainDispatcherRule.testDispatcher) {
        repository.clearReunion()
        val viewModel = create()
        runCurrent()

        viewModel.onSave()

        assertEquals(EditReunionProblem.DateMissing, viewModel.uiState.value.problem)
        assertFalse(viewModel.uiState.value.isDone)
    }

    @Test
    fun aTimeEarlierTodayIsInThePast() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = create()
        runCurrent()

        viewModel.onDateSelected(LocalDate.of(2026, 9, 26))
        viewModel.onTimeSelected(LocalTime.of(9, 0))
        viewModel.onSave()

        assertEquals(EditReunionProblem.InPast, viewModel.uiState.value.problem)
    }

    @Test
    fun todayWithoutATimeIsAllowed() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = create()
        runCurrent()

        viewModel.onDateSelected(LocalDate.of(2026, 9, 26))
        viewModel.onHasTimeChange(false)
        viewModel.onSave()
        runCurrent()

        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun turningTheTimeOnOpensThePicker() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = create()
        runCurrent()

        viewModel.onHasTimeChange(true)

        assertEquals(ReunionPicker.Time, viewModel.uiState.value.openPicker)
    }

    @Test
    fun placeAndNoteAreLimited() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = create()
        runCurrent()

        viewModel.onPlaceChange("p".repeat(150))
        viewModel.onNoteChange("n".repeat(600))

        assertEquals(ReunionPlan.MaxPlaceLength, viewModel.uiState.value.place.length)
        assertEquals(ReunionPlan.MaxNoteLength, viewModel.uiState.value.note.length)
    }

    @Test
    fun clearingNeedsConfirmation() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = create()
        runCurrent()

        viewModel.onClearRequested()
        assertTrue(viewModel.uiState.value.isClearConfirmOpen)

        viewModel.onClearConfirmed()
        runCurrent()

        assertNull(repository.reunion.value)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun saveFailuresAreShown() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = create()
        runCurrent()
        repository.failNextWith = DataError.Network

        viewModel.onDateSelected(LocalDate.of(2026, 10, 17))
        viewModel.onSave()
        runCurrent()

        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isDone)
    }
}
