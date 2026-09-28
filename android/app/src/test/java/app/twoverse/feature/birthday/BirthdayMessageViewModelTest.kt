package app.twoverse.feature.birthday

import app.twoverse.core.data.fake.FakeBirthdayRepository
import app.twoverse.core.model.BirthdayMessage
import app.twoverse.core.model.BirthdayMessageDraft
import app.twoverse.core.model.BirthdayPhotoChange
import app.twoverse.core.model.DataError
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
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class BirthdayMessageViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBirthdayRepository()
    private val clock = Clock.fixed(Instant.parse("2026-09-28T09:00:00Z"), ZoneId.of("Asia/Colombo"))
    private val today = LocalDate.of(2026, 9, 28)

    private fun createViewModel() = BirthdayMessageViewModel(repository, clock)

    @Test
    fun startsEmptyWhenNothingWasWritten() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.message)
        assertFalse(state.canSave)
    }

    @Test
    fun loadsTheSavedWelcome() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setMyMessage(BirthdayMessage("Happy birthday", today.plusDays(5), photoUrl = "https://photo", seen = true))

        val viewModel = createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("Happy birthday", state.message)
        assertEquals(today.plusDays(5), state.showOn)
        assertEquals("https://photo", state.photo)
        assertTrue(state.wasSeen)
    }

    @Test
    fun savingSendsTheMessageDateAndNewPhoto() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onMessageChange("  Happy birthday  ")
        viewModel.onPhotoPicked("content://photo")
        viewModel.onDateSelected(today.plusDays(3))
        viewModel.onSave()
        runCurrent()

        assertEquals(
            listOf(BirthdayMessageDraft("Happy birthday", today.plusDays(3), BirthdayPhotoChange.Replace("content://photo"))),
            repository.savedDrafts,
        )
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun theSavedPhotoIsKeptOrRemoved() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setMyMessage(BirthdayMessage("Hi", null, photoUrl = "https://photo", seen = false))
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onSave()
        runCurrent()
        viewModel.onRemovePhoto()
        viewModel.onSave()
        runCurrent()

        assertEquals(listOf(BirthdayPhotoChange.Keep, BirthdayPhotoChange.Remove), repository.savedDrafts.map { it.photo })
        assertNull(viewModel.uiState.value.photo)
    }

    @Test
    fun aPastDateCannotBeChosenAndTheDateCanBeCleared() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onDateSelected(today.minusDays(1))
        assertNull(viewModel.uiState.value.showOn)
        viewModel.onDateSelected(today)
        assertEquals(today, viewModel.uiState.value.showOn)
        viewModel.onClearDate()
        assertNull(viewModel.uiState.value.showOn)
    }

    @Test
    fun messageIsLimitedTo1000Characters() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onMessageChange("a".repeat(1200))

        assertEquals(1000, viewModel.uiState.value.message.length)
    }

    @Test
    fun aFailedSaveKeepsTheTextAndShowsTheError() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onMessageChange("Happy birthday")
        repository.failNextWith(DataError.Network)

        viewModel.onSave()
        runCurrent()

        assertFalse(viewModel.uiState.value.isSaved)
        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertEquals("Happy birthday", viewModel.uiState.value.message)
    }

    @Test
    fun whenLoadingFailsSavingIsBlockedUntilRetried() = runTest(mainDispatcherRule.testDispatcher) {
        repository.failNextWith(DataError.Network)
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onMessageChange("Happy birthday")

        assertEquals(DataError.Network, viewModel.uiState.value.loadError)
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.load()
        runCurrent()

        assertNull(viewModel.uiState.value.loadError)
    }
}
