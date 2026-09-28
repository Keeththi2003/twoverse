package app.twoverse.feature.countdown

import app.twoverse.core.common.CountdownTime
import app.twoverse.core.data.fake.FakeReunionRepository
import app.twoverse.core.model.ReunionPlan
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CountdownViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val colombo = ZoneId.of("Asia/Colombo")

    /** 01:35:50 local time in Colombo (UTC+5:30). */
    private var start = Instant.parse("2026-09-25T20:05:50Z")

    /** Saturday 10 October 2026, 10:00 in Colombo, stored as UTC. */
    private val meetAt = Instant.parse("2026-10-10T04:30:00Z")

    private val repository = FakeReunionRepository()

    /** A clock that moves forward with the test dispatcher's virtual time. */
    private fun TestScope.virtualClock() = object : Clock() {
        override fun getZone(): ZoneId = colombo
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = start.plusMillis(testScheduler.currentTime)
    }

    private fun TestScope.createViewModel(): CountdownViewModel {
        val viewModel = CountdownViewModel(repository, virtualClock())
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    private suspend fun plan(meetAt: Instant, hasTime: Boolean = true, note: String? = null) {
        repository.saveReunion(ReunionPlan(meetAt = meetAt, hasTime = hasTime, place = "Kandy", note = note))
    }

    @Test
    fun showsTimeLeftAndTheLocalPlan() = runTest(mainDispatcherRule.testDispatcher) {
        plan(meetAt, note = "Bring the camera")
        val viewModel = createViewModel()
        runCurrent()

        val content = viewModel.uiState.value.content as CountdownContent.Counting
        assertEquals(CountdownTime(days = 14, hours = 8, minutes = 24, seconds = 10), content.timeLeft)
        assertEquals(ReunionDetails(LocalDate.of(2026, 10, 10), LocalTime.of(10, 0), "Kandy", "Bring the camera"), content.plan)
    }

    @Test
    fun secondsTickLive() = runTest(mainDispatcherRule.testDispatcher) {
        plan(meetAt)
        val viewModel = createViewModel()
        runCurrent()

        advanceTimeBy(3_000)
        runCurrent()

        val content = viewModel.uiState.value.content as CountdownContent.Counting
        assertEquals(7, content.timeLeft.seconds)
    }

    @Test
    fun dateWithoutTimeHasNoTime() = runTest(mainDispatcherRule.testDispatcher) {
        plan(meetAt, hasTime = false)
        val viewModel = createViewModel()
        runCurrent()

        assertEquals(null, (viewModel.uiState.value.content as CountdownContent.Counting).plan.time)
    }

    @Test
    fun noReunionShowsTheEmptyState() = runTest(mainDispatcherRule.testDispatcher) {
        repository.clearReunion()
        val viewModel = createViewModel()
        runCurrent()

        assertEquals(CountdownContent.NoDate, viewModel.uiState.value.content)
    }

    @Test
    fun reachingZeroCelebratesUntilTheEndOfTheDay() = runTest(mainDispatcherRule.testDispatcher) {
        start = meetAt
        plan(meetAt)
        val viewModel = createViewModel()
        runCurrent()
        assertEquals(CountdownContent.Celebrating::class, viewModel.uiState.value.content::class)

        advanceTimeBy(Duration.ofHours(14).toMillis())
        runCurrent()
        assertEquals(CountdownContent.AfterReunion, viewModel.uiState.value.content)
    }

    @Test
    fun aPartnersChangeShowsUpLive() = runTest(mainDispatcherRule.testDispatcher) {
        plan(meetAt)
        val viewModel = createViewModel()
        runCurrent()

        plan(meetAt.plus(Duration.ofDays(7)))
        runCurrent()

        assertEquals(LocalDate.of(2026, 10, 17), (viewModel.uiState.value.content as CountdownContent.Counting).plan.date)
    }
}
