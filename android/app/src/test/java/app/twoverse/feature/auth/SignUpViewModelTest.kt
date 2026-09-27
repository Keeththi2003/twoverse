package app.twoverse.feature.auth

import app.twoverse.core.data.fake.FakeAuthRepository
import app.twoverse.core.model.AuthState
import app.twoverse.core.model.DataError
import app.twoverse.core.model.SignUpResult
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
class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val viewModel = SignUpViewModel(authRepository)

    private fun fillForm(name: String = "Keeththi", email: String = "you@example.com", password: String = "secret1") {
        viewModel.onDisplayNameChange(name)
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
    }

    @Test
    fun signsUpAndSignsIn() = runTest(mainDispatcherRule.testDispatcher) {
        fillForm()
        viewModel.onSignUp()
        runCurrent()

        assertTrue(viewModel.uiState.value.isSignedUp)
        assertTrue(authRepository.authState.value is AuthState.SignedIn)
    }

    @Test
    fun emailConfirmationKeepsTheUserHere() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signUpResult = SignUpResult.ConfirmEmail
        fillForm()
        viewModel.onSignUp()
        runCurrent()

        assertTrue(viewModel.uiState.value.isConfirmationSent)
        assertFalse(viewModel.uiState.value.isSignedUp)
    }

    @Test
    fun blankNameIsRejectedBeforeCallingTheServer() {
        fillForm(name = "   ")
        viewModel.onSignUp()

        assertEquals(SignUpProblem.NameMissing, viewModel.uiState.value.problem)
        assertEquals(AuthState.SignedOut, authRepository.authState.value)
    }

    @Test
    fun malformedEmailIsRejected() {
        fillForm(email = "not-an-email")
        viewModel.onSignUp()

        assertEquals(SignUpProblem.EmailInvalid, viewModel.uiState.value.problem)
    }

    @Test
    fun shortPasswordIsRejected() {
        fillForm(password = "12345")
        viewModel.onSignUp()

        assertEquals(DataError.WeakPassword, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.problem)
    }

    @Test
    fun nameIsLimitedToFiftyCharacters() {
        viewModel.onDisplayNameChange("x".repeat(60))

        assertEquals(SignUpViewModel.MaxNameLength, viewModel.uiState.value.displayName.length)
    }

    @Test
    fun serverErrorsAreShown() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.failNextWith = DataError.EmailInUse
        fillForm()
        viewModel.onSignUp()
        runCurrent()

        assertEquals(DataError.EmailInUse, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
