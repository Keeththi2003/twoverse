package app.twoverse.feature.countdown

import app.twoverse.core.common.CountdownTime
import app.twoverse.core.data.fake.FakeReunionRepository
import app.twoverse.core.model.Reunion
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
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
    private val start = Instant.parse("2026-09-25T20:05:50Z")

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

    private suspend fun setReunion(meetAt: Instant, hasTime: Boolean = true) {
        repository.setReunion(
            Reunion(meetAt = meetAt, hasTime = hasTime, place = "Kandy", note = null, updatedAt = start),
        )
    }

    private fun CountdownViewModel.counting() = uiState.value.content as CountdownContent.Counting

    @Test
    fun showsTimeLeftAndLocalDateAndTime() = runTest(mainDispatcherRule.testDispatcher) {
        setReunion(meetAt)
        val viewModel = createViewModel()
        runCurrent()

        val content = viewModel.counting()
        assertEquals(CountdownTime(days = 14, hours = 8, minutes = 24, seconds = 10), content.timeLeft)
        assertEquals(LocalDate.of(2026, 10, 10), content.date)
        assertEquals(LocalTime.of(10, 0), content.time)
        assertEquals("Kandy", content.place)
    }

    @Test
    fun secondsTickLive() = runTest(mainDispatcherRule.testDispatcher) {
        setReunion(meetAt)
        val viewModel = createViewModel()
        runCurrent()

        advanceTimeBy(3_000)
        runCurrent()

        assertEquals(CountdownTime(days = 14, hours = 8, minutes = 24, seconds = 7), viewModel.counting().timeLeft)
    }

    @Test
    fun dateWithoutTimeHasNoTime() = runTest(mainDispatcherRule.testDispatcher) {
        setReunion(meetAt, hasTime = false)
        val viewModel = createViewModel()
        runCurrent()

        assertEquals(null, viewModel.counting().time)
    }

    @Test
    fun noReunionShowsEmptyState() = runTest(mainDispatcherRule.testDispatcher) {
        repository.clearReunion()
        val viewModel = createViewModel()
        runCurrent()

        assertEquals(CountdownContent.NoDate(afterReunion = false), viewModel.uiState.value.content)
    }

    @Test
    fun reachedReunionAsksForTheNextOne() = runTest(mainDispatcherRule.testDispatcher) {
        setReunion(start.minusSeconds(60))
        val viewModel = createViewModel()
        runCurrent()

        assertEquals(CountdownContent.NoDate(afterReunion = true), viewModel.uiState.value.content)
    }

    @Test
    fun changingDateKeepsLocalTimeAndStoresUtc() = runTest(mainDispatcherRule.testDispatcher) {
        setReunion(meetAt)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onChangeDate()
        runCurrent()
        assertTrue(viewModel.uiState.value.isDatePickerOpen)

        viewModel.onDateSelected(LocalDate.of(2026, 10, 17))
        runCurrent()

        val saved = repository.reunion.value
        assertEquals(Instant.parse("2026-10-17T04:30:00Z"), saved?.meetAt)
        assertEquals(LocalDate.of(2026, 10, 17).atTime(10, 0).atZone(colombo).toInstant(), saved?.meetAt)
        assertEquals(false, viewModel.uiState.value.isDatePickerOpen)
        assertEquals(LocalDate.of(2026, 10, 17), viewModel.counting().date)
    }
}
