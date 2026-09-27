package app.twoverse.feature.auth

import app.twoverse.core.data.fake.FakeAuthRepository
import app.twoverse.core.model.DataError
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResetPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val viewModel = ResetPasswordViewModel(authRepository)

    @Test
    fun savesTheNewPassword() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onPasswordChange("new-secret")
        viewModel.onSave()
        runCurrent()

        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun shortPasswordIsRejected() {
        viewModel.onPasswordChange("123")
        viewModel.onSave()

        assertEquals(DataError.WeakPassword, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun serverErrorsAreShown() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.failNextWith = DataError.Network
        viewModel.onPasswordChange("new-secret")
        viewModel.onSave()
        runCurrent()

        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }
}
