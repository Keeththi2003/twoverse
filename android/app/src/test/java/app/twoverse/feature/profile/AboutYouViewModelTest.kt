package app.twoverse.feature.profile

import app.twoverse.core.data.fake.FakeProfileRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.MyProfile
import app.twoverse.core.model.Pronouns
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AboutYouViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profileRepository = FakeProfileRepository().apply {
        setMyProfile(
            MyProfile(
                fullName = "Keeththi Lan",
                shortName = "Keeththi",
                pronouns = null,
                phone = null,
                shareEmail = false,
                sharePhone = false,
                email = "k@example.com",
                pendingEmail = null,
                canChangeEmail = false,
            ),
        )
    }

    private fun TestScope.createViewModel(): AboutYouViewModel = AboutYouViewModel(profileRepository).also { runCurrent() }

    @Test
    fun theShortNameIsPrefilledAndPronounsAreNotChosen() = runTest(mainDispatcherRule.testDispatcher) {
        val state = createViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("Keeththi", state.shortName)
        assertNull(state.pronouns)
    }

    @Test
    fun waitsForTheProfileBeforeShowingTheForm() = runTest(mainDispatcherRule.testDispatcher) {
        profileRepository.setMyProfile(null)
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.onContinue()
        assertNull(viewModel.uiState.value.problem)
    }

    @Test
    fun pronounsMustBeChosen() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onContinue()

        assertEquals(AboutYouProblem.PronounsMissing, viewModel.uiState.value.problem)
        assertFalse(viewModel.uiState.value.isDone)
    }

    @Test
    fun aBlankNameIsNotAccepted() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onPronounsSelected(Pronouns.He)
        viewModel.onShortNameChange("   ")

        viewModel.onContinue()

        assertEquals(AboutYouProblem.NameMissing, viewModel.uiState.value.problem)
    }

    @Test
    fun theShortNameIsCappedAt30Characters() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onShortNameChange("x".repeat(40))

        assertEquals(30, viewModel.uiState.value.shortName.length)
    }

    @Test
    fun continueSavesTheTrimmedNameAndPronouns() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onShortNameChange("  Kee ")
        viewModel.onPronounsSelected(Pronouns.They)

        viewModel.onContinue()
        runCurrent()

        assertTrue(viewModel.uiState.value.isDone)
        assertEquals("Kee", profileRepository.myProfile.value?.shortName)
        assertEquals(Pronouns.They, profileRepository.myProfile.value?.pronouns)
    }

    @Test
    fun aFailedSaveShowsTheErrorAndStays() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onPronounsSelected(Pronouns.She)
        profileRepository.failNextWith(DataError.Network)

        viewModel.onContinue()
        runCurrent()

        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isDone)
        assertFalse(viewModel.uiState.value.isSaving)
    }
}
