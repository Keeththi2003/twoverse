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
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profile = MyProfile(
        fullName = "Keeththi Lan",
        shortName = "Keeththi",
        pronouns = Pronouns.He,
        phone = "+94771234567",
        shareEmail = false,
        sharePhone = true,
        email = "k@example.com",
        pendingEmail = null,
        canChangeEmail = true,
    )
    private val profileRepository = FakeProfileRepository().apply { setMyProfile(profile) }

    private fun TestScope.createViewModel(): ProfileViewModel = ProfileViewModel(profileRepository).also { runCurrent() }

    @Test
    fun theFormStartsFromMyProfile() = runTest(mainDispatcherRule.testDispatcher) {
        val state = createViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("Keeththi Lan", state.fullName)
        assertEquals("Keeththi", state.shortName)
        assertEquals(Pronouns.He, state.pronouns)
        assertEquals("+94771234567", state.phone)
        assertTrue(state.sharePhone)
        assertEquals("k@example.com", state.email)
        assertTrue(state.canChangeEmail)
    }

    @Test
    fun savingStoresTheEditsWithThePhoneInInternationalFormat() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onFullNameChange(" Keeththi L ")
        viewModel.onShortNameChange("Kee")
        viewModel.onPronounsSelected(Pronouns.They)
        viewModel.onPhoneChange("077 765 4321")
        viewModel.onShareEmailChange(true)

        viewModel.onSave()
        runCurrent()

        val saved = profileRepository.myProfile.value
        assertEquals("Keeththi L", saved?.fullName)
        assertEquals("Kee", saved?.shortName)
        assertEquals(Pronouns.They, saved?.pronouns)
        assertEquals("+94777654321", saved?.phone)
        assertEquals(true, saved?.shareEmail)
        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals("+94777654321", viewModel.uiState.value.phone)
    }

    @Test
    fun anInvalidPhoneIsNotSaved() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onPhoneChange("0771")

        viewModel.onSave()
        runCurrent()

        assertEquals(ProfileProblem.PhoneInvalid, viewModel.uiState.value.problem)
        assertEquals("+94771234567", profileRepository.myProfile.value?.phone)
    }

    @Test
    fun clearingThePhoneStopsSharingIt() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onPhoneChange("  ")

        viewModel.onSave()
        runCurrent()

        assertNull(profileRepository.myProfile.value?.phone)
        assertEquals(false, profileRepository.myProfile.value?.sharePhone)
        assertFalse(viewModel.uiState.value.sharePhone)
    }

    @Test
    fun namesAreRequired() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onFullNameChange(" ")
        viewModel.onSave()
        assertEquals(ProfileProblem.FullNameMissing, viewModel.uiState.value.problem)

        viewModel.onFullNameChange("Keeththi Lan")
        assertNull(viewModel.uiState.value.problem)
        viewModel.onShortNameChange("")
        viewModel.onSave()
        assertEquals(ProfileProblem.ShortNameMissing, viewModel.uiState.value.problem)
    }

    @Test
    fun theShortNameIsCappedAt30Characters() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onShortNameChange("x".repeat(40))

        assertEquals(30, viewModel.uiState.value.shortName.length)
    }

    @Test
    fun aFailedSaveShowsTheError() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        profileRepository.failNextWith(DataError.Network)

        viewModel.onSave()
        runCurrent()

        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun aNewEmailWaitsForConfirmationAndTheOldOneStays() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onChangeEmail()
        assertTrue(viewModel.uiState.value.isEmailDialogOpen)

        viewModel.onNewEmailChange(" new@example.com ")
        viewModel.onConfirmEmail()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isEmailDialogOpen)
        assertTrue(state.isEmailChangeSent)
        assertEquals("k@example.com", state.email)
        assertEquals("new@example.com", state.pendingEmail)
    }

    @Test
    fun anInvalidOrUnchangedEmailIsRefused() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onChangeEmail()

        viewModel.onNewEmailChange("not-an-email")
        viewModel.onConfirmEmail()
        assertTrue(viewModel.uiState.value.isNewEmailInvalid)
        assertTrue(viewModel.uiState.value.isEmailDialogOpen)

        viewModel.onNewEmailChange("K@example.com")
        viewModel.onConfirmEmail()
        runCurrent()
        assertTrue(viewModel.uiState.value.isNewEmailInvalid)
        assertNull(profileRepository.myProfile.value?.pendingEmail)
    }

    @Test
    fun typingIsNotOverwrittenWhenTheProfileRefreshes() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onShortNameChange("Kee")

        profileRepository.setMyProfile(profile.copy(pendingEmail = "new@example.com"))
        runCurrent()

        assertEquals("Kee", viewModel.uiState.value.shortName)
        assertEquals("new@example.com", viewModel.uiState.value.pendingEmail)
    }

    @Test
    fun emailValidation() {
        assertTrue(isValidEmail("a@b.co"))
        assertFalse(isValidEmail("a@b"))
        assertFalse(isValidEmail("a b@c.de"))
        assertFalse(isValidEmail(""))
    }
}
