package app.twoverse.feature.birthday

import app.twoverse.core.data.fake.FakeBirthdayRepository
import app.twoverse.core.model.BirthdayWelcome
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BirthdayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBirthdayRepository()

    private fun TestScope.createViewModel(): BirthdayViewModel {
        val viewModel = BirthdayViewModel(repository)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    @Test
    fun withoutAWelcomeTheScreenContinues() = runTest(mainDispatcherRule.testDispatcher) {
        assertEquals(BirthdayUiState.None, createViewModel().uiState.value)
    }

    @Test
    fun showsTheMessageAndPhotoFromThePartner() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setWelcome(BirthdayWelcome("Happy birthday", "Her", photoUrl = "https://photo", showOn = null, seen = false))

        assertEquals(
            BirthdayUiState.Welcome(message = "Happy birthday", fromName = "Her", photoUrl = "https://photo"),
            createViewModel().uiState.value,
        )
    }

    @Test
    fun enteringMarksItSeenSoItShowsOnce() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setWelcome(BirthdayWelcome("Happy birthday", "Her", photoUrl = null, showOn = null, seen = false))
        val viewModel = createViewModel()

        viewModel.onEnter()
        runCurrent()

        assertTrue(repository.welcome.value?.seen == true)
        assertTrue((viewModel.uiState.value as BirthdayUiState.Welcome).isEntered)
    }
}
