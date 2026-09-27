package app.twoverse.feature.auth

import app.twoverse.core.data.fake.FakeAuthRepository
import app.twoverse.core.data.fake.FakeCoupleRepository
import app.twoverse.core.model.AuthState
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

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val coupleRepository = FakeCoupleRepository()
    private val viewModel = SignInViewModel(authRepository, coupleRepository)

    private fun fillForm() {
        viewModel.onEmailChange(" you@example.com ")
        viewModel.onPasswordChange("secret1")
    }

    @Test
    fun signInNeedsEmailAndPassword() {
        assertFalse(viewModel.uiState.value.canSubmit)
        fillForm()
        assertTrue(viewModel.uiState.value.canSubmit)
        assertEquals("you@example.com", viewModel.uiState.value.email)
    }

    @Test
    fun unpairedUserContinuesToPairing() = runTest(mainDispatcherRule.testDispatcher) {
        fillForm()
        viewModel.onEmailSignIn()
        runCurrent()

        assertTrue(authRepository.authState.value is AuthState.SignedIn)
        assertEquals(SignedInDestination.Pair, viewModel.uiState.value.destination)
    }

    @Test
    fun pairedUserGoesStraightHome() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.join("AB12-CD34")
        viewModel.onGoogleIdToken("id-token", "nonce")
        runCurrent()

        assertEquals(SignedInDestination.Home, viewModel.uiState.value.destination)
    }

    @Test
    fun failedSignInShowsTheMappedError() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.failNextWith = DataError.InvalidCredentials
        fillForm()
        viewModel.onEmailSignIn()
        runCurrent()

        assertEquals(DataError.InvalidCredentials, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.destination)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun typingClearsTheError() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.failNextWith = DataError.Network
        fillForm()
        viewModel.onEmailSignIn()
        runCurrent()

        viewModel.onPasswordChange("secret2")

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun googleFailureShowsAMessage() {
        viewModel.onGoogleSignInFailed()

        assertEquals(SignInMessage.GoogleFailed, viewModel.uiState.value.message)
    }

    @Test
    fun passwordResetNeedsAnEmail() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onForgotPassword()
        assertEquals(SignInMessage.ResetNeedsEmail, viewModel.uiState.value.message)

        viewModel.onEmailChange("you@example.com")
        viewModel.onForgotPassword()
        runCurrent()
        assertEquals(SignInMessage.ResetSent, viewModel.uiState.value.message)
    }

    @Test
    fun failedPasswordResetShowsTheError() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.failNextWith = DataError.RateLimited
        viewModel.onEmailChange("you@example.com")
        viewModel.onForgotPassword()
        runCurrent()

        assertEquals(DataError.RateLimited, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.message)
    }
}
