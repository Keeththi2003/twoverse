package app.twoverse.feature.orbit

import app.twoverse.core.common.Milestone
import app.twoverse.core.data.fake.FakeOrbitRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.Meetup
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class OrbitViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeOrbitRepository()
    private val zone = ZoneId.of("Asia/Colombo")

    private fun clockOn(date: LocalDate): Clock = Clock.fixed(date.atTime(12, 0).atZone(zone).toInstant(), zone)

    private fun TestScope.createViewModel(today: LocalDate = LocalDate.of(2026, 5, 19)): OrbitViewModel {
        val viewModel = OrbitViewModel(repository, clockOn(today))
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    private fun meetup(id: String, start: String, end: String? = null) =
        Meetup(id, LocalDate.parse(start), end?.let(LocalDate::parse), place = null, note = null)

    @Test
    fun withoutADateOnlyTheMeetupsShow() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setMeetups(listOf(meetup("a", "2026-05-01")))

        val state = createViewModel().uiState.value

        assertFalse(state.isLoading)
        assertNull(state.togetherSince)
        assertNull(state.duration)
        assertNull(state.nextAnniversary)
        assertEquals(1, state.stats.timesMet)
    }

    @Test
    fun showsTheDurationAnniversaryAndNextDayMilestone() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setTogetherSince(LocalDate.of(2024, 2, 14))

        val state = createViewModel().uiState.value

        assertEquals(Period.of(2, 3, 5), state.duration?.period)
        assertEquals(826L, state.duration?.totalDays)
        assertEquals(3, state.nextAnniversary?.years)
        assertEquals(LocalDate.of(2027, 2, 14), state.nextAnniversary?.date)
        // Day 1000 is 999 days after 14 February 2024.
        assertEquals(Milestone.Days(1000), state.nextDayMilestone?.milestone)
        assertEquals(LocalDate.of(2026, 11, 9), state.nextDayMilestone?.date)
        assertNull(state.celebration)
    }

    @Test
    fun theMilestoneCardAlwaysShowsADayCountEvenWhenTheAnniversaryComesFirst() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setTogetherSince(LocalDate.of(2024, 1, 1))

        val state = createViewModel(today = LocalDate.of(2024, 12, 31)).uiState.value

        assertEquals(Milestone.Days(500), state.nextDayMilestone?.milestone)
        assertEquals(1L, state.nextAnniversary?.daysUntil)
    }

    @Test
    fun theMilestoneBarShowsTodayAgainstTheNextMilestone() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setTogetherSince(LocalDate.of(2025, 6, 22))

        // 9 October 2026 is day 475 since 22 June 2025.
        val progress = createViewModel(today = LocalDate.of(2026, 10, 9)).uiState.value.milestoneProgress

        assertEquals(MilestoneProgress(day = 475, milestone = 500), progress)
        assertEquals(0.95f, progress?.fraction ?: 0f, 0.0001f)
    }

    @Test
    fun withoutADateThereIsNoMilestoneBar() = runTest(mainDispatcherRule.testDispatcher) {
        assertNull(createViewModel().uiState.value.milestoneProgress)
    }

    @Test
    fun celebratesTheAnniversary() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setTogetherSince(LocalDate.of(2024, 2, 14))

        val state = createViewModel(today = LocalDate.of(2026, 2, 14)).uiState.value

        assertEquals(Celebration.Anniversary(years = 2), state.celebration)
    }

    @Test
    fun celebratesADayMilestone() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setTogetherSince(LocalDate.of(2024, 2, 14))

        val state = createViewModel(today = LocalDate.of(2024, 5, 23)).uiState.value

        assertEquals(Celebration.DayMilestone(day = 100), state.celebration)
    }

    @Test
    fun meetupsAreNewestFirstWithTheirStats() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setMeetups(
            listOf(
                meetup("old", "2026-01-01", "2026-01-03"),
                meetup("new", "2026-05-07", "2026-05-09"),
                meetup("mid", "2026-03-10"),
            ),
        )

        val state = createViewModel().uiState.value

        assertEquals(listOf("new", "mid", "old"), state.meetups.map { it.id })
        assertEquals(3, state.stats.timesMet)
        assertEquals(7L, state.stats.daysTogether)
        assertEquals(10L, state.stats.daysSinceLastMet)
    }

    @Test
    fun deletingAsksFirst() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setMeetups(listOf(meetup("a", "2026-05-01")))
        val viewModel = createViewModel()

        viewModel.onDelete("a")
        runCurrent()
        assertEquals("a", viewModel.uiState.value.pendingDeleteId)
        viewModel.onDeleteDismissed()
        runCurrent()
        assertEquals(1, viewModel.uiState.value.meetups.size)

        viewModel.onDelete("a")
        viewModel.onDeleteConfirmed()
        runCurrent()
        assertEquals(0, viewModel.uiState.value.meetups.size)
    }

    @Test
    fun aFailedDeleteIsReported() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setMeetups(listOf(meetup("a", "2026-05-01")))
        val viewModel = createViewModel()
        repository.failNextWith(DataError.Network)

        viewModel.onDelete("a")
        viewModel.onDeleteConfirmed()
        runCurrent()

        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertEquals(1, viewModel.uiState.value.meetups.size)
    }
}
