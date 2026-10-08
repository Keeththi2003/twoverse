package app.twoverse.feature.orbit

import androidx.lifecycle.SavedStateHandle
import app.twoverse.core.data.fake.FakeOrbitRepository
import app.twoverse.core.data.fake.FakeReunionRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.Meetup
import app.twoverse.core.model.MeetupDraft
import app.twoverse.core.model.ReunionPlan
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
class MeetupEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val orbitRepository = FakeOrbitRepository()
    private val reunionRepository = FakeReunionRepository()
    private val zone = ZoneId.of("Asia/Colombo")
    private val clock = Clock.fixed(Instant.parse("2026-10-12T09:00:00Z"), zone)
    private val today = LocalDate.of(2026, 10, 12)

    private fun TestScope.createViewModel(meetupId: String? = null, fromReunion: Boolean = false): MeetupEditorViewModel {
        val handle = SavedStateHandle(mapOf(MeetupIdKey to meetupId, FromReunionKey to fromReunion))
        val viewModel = MeetupEditorViewModel(handle, orbitRepository, reunionRepository, clock)
        runCurrent()
        return viewModel
    }

    private suspend fun saved(): List<Meetup> = orbitRepository.meetups.first()

    @Test
    fun addsAOneDayMeetup() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onStartDateSelected(LocalDate.of(2026, 10, 3))
        viewModel.onPlaceChange("  Kandy ")
        viewModel.onNoteChange("   ")

        viewModel.onSave()
        runCurrent()

        assertTrue(viewModel.uiState.value.isSaved)
        val meetup = saved().single()
        assertEquals(LocalDate.of(2026, 10, 3), meetup.startDate)
        assertNull(meetup.endDate)
        assertEquals("Kandy", meetup.place)
        assertNull(meetup.note)
    }

    @Test
    fun aVisitOfSeveralDaysHasAnEndDate() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onStartDateSelected(LocalDate.of(2026, 10, 3))
        viewModel.onSeveralDaysChange(true)
        assertEquals(MeetupPicker.End, viewModel.uiState.value.openPicker)
        viewModel.onEndDateSelected(LocalDate.of(2026, 10, 6))

        viewModel.onSave()
        runCurrent()

        assertEquals(LocalDate.of(2026, 10, 6), saved().single().endDate)
    }

    @Test
    fun turningSeveralDaysOffDropsTheEndDate() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onStartDateSelected(LocalDate.of(2026, 10, 3))
        viewModel.onEndDateSelected(LocalDate.of(2026, 10, 6))
        viewModel.onSeveralDaysChange(false)

        viewModel.onSave()
        runCurrent()

        assertNull(saved().single().endDate)
    }

    @Test
    fun theDatesAreChecked() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onSave()
        assertEquals(MeetupProblem.StartMissing, viewModel.uiState.value.problem)

        viewModel.onStartDateSelected(today.plusDays(1))
        viewModel.onSave()
        assertEquals(MeetupProblem.StartInFuture, viewModel.uiState.value.problem)

        viewModel.onStartDateSelected(LocalDate.of(2026, 10, 5))
        viewModel.onEndDateSelected(LocalDate.of(2026, 10, 4))
        viewModel.onSave()
        assertEquals(MeetupProblem.EndBeforeStart, viewModel.uiState.value.problem)

        runCurrent()
        assertTrue(saved().isEmpty())
    }

    @Test
    fun aMeetupCanStartToday() {
        assertNull(meetupProblem(today, null, today))
        assertNull(meetupProblem(today, today.plusDays(2), today))
    }

    @Test
    fun textIsLimitedToWhatTheBackendAccepts() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onPlaceChange("p".repeat(MeetupDraft.MaxPlaceLength + 5))
        viewModel.onNoteChange("n".repeat(MeetupDraft.MaxNoteLength + 5))

        assertEquals(MeetupDraft.MaxPlaceLength, viewModel.uiState.value.place.length)
        assertEquals(MeetupDraft.MaxNoteLength, viewModel.uiState.value.note.length)
    }

    @Test
    fun editingLoadsAndSavesOverTheMeetup() = runTest(mainDispatcherRule.testDispatcher) {
        orbitRepository.setMeetups(listOf(Meetup("m", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), "Galle", "Beach")))
        val viewModel = createViewModel(meetupId = "m")

        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertTrue(state.severalDays)
        assertEquals("Galle", state.place)

        viewModel.onNoteChange("Beach and rain")
        viewModel.onSave()
        runCurrent()

        assertEquals("Beach and rain", saved().single { it.id == "m" }.note)
        assertEquals(1, saved().size)
    }

    @Test
    fun aMeetupDeletedMeanwhileCannotBeEdited() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(meetupId = "gone")

        assertEquals(DataError.Unknown, viewModel.uiState.value.loadError)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun recordingAPassedReunionFillsInItsDateAndPlace() = runTest(mainDispatcherRule.testDispatcher) {
        val meetAt = Instant.parse("2026-10-09T20:00:00Z")
        reunionRepository.saveReunion(ReunionPlan(meetAt = meetAt, hasTime = true, place = "Kandy", note = null))
        val viewModel = createViewModel(fromReunion = true)

        val state = viewModel.uiState.value
        // 20:00 UTC on 9 October is 01:30 on 10 October in Colombo.
        assertEquals(LocalDate.of(2026, 10, 10), state.startDate)
        assertEquals("Kandy", state.place)

        viewModel.onSeveralDaysChange(true)
        viewModel.onEndDateSelected(LocalDate.of(2026, 10, 12))
        viewModel.onSave()
        runCurrent()

        val meetup = saved().single()
        assertEquals(meetAt, meetup.fromReunionAt)
        assertEquals(LocalDate.of(2026, 10, 12), meetup.endDate)
    }

    @Test
    fun aFailedSaveKeepsEverythingForARetry() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onStartDateSelected(LocalDate.of(2026, 10, 3))
        viewModel.onPlaceChange("Kandy")
        orbitRepository.failNextWith(DataError.Network)

        viewModel.onSave()
        runCurrent()

        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
        assertEquals("Kandy", viewModel.uiState.value.place)
    }
}
