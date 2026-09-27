package app.twoverse.feature.settings

import app.twoverse.core.data.fake.FakeAuthRepository
import app.twoverse.core.data.fake.FakeCoupleRepository
import app.twoverse.core.data.fake.FakeLocationRepository
import app.twoverse.core.data.settings.DefaultSettingsRepository
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.AuthState
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import app.twoverse.testing.InMemoryUserPreferences
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = InMemoryUserPreferences()
    private val locationRepository = FakeLocationRepository()
    private val settingsRepository = DefaultSettingsRepository(locationRepository, preferences)
    private val coupleRepository = FakeCoupleRepository()
    private val authRepository = FakeAuthRepository()
    private val clock = Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneId.of("Asia/Colombo"))

    private fun TestScope.createViewModel(): SettingsViewModel {
        val viewModel = SettingsViewModel(settingsRepository, coupleRepository, authRepository, clock)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    @Test
    fun showsSettingsAndCouple() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.join("AB12-CD34")
        val state = createViewModel().uiState.value

        assertEquals(true, state.settings?.shareLocation)
        assertTrue(state.isConnected)
        assertEquals(LocalDate.of(2026, 2, 14), state.connectedSince)
    }

    @Test
    fun switchesUpdateSettings() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onShareLocationChange(false)
        viewModel.onLockOursChange(false)
        runCurrent()

        assertEquals(false, viewModel.uiState.value.settings?.shareLocation)
        assertEquals(false, viewModel.uiState.value.settings?.lockOurs)
    }

    @Test
    fun choosingAnOptionSavesItAndClosesTheDialog() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onOpenDialog(SettingsDialog.Appearance)
        runCurrent()
        assertEquals(SettingsDialog.Appearance, viewModel.uiState.value.openDialog)

        viewModel.onAppearanceSelected(AppearanceMode.Dark)
        runCurrent()

        assertNull(viewModel.uiState.value.openDialog)
        assertEquals(AppearanceMode.Dark, viewModel.uiState.value.settings?.appearance)
        assertEquals(AppearanceMode.Dark, preferences.appearance.value)
    }

    @Test
    fun distanceUnitAndPrecisionAreSaved() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onDistanceUnitSelected(DistanceUnit.Miles)
        viewModel.onLocationPrecisionSelected(LocationPrecision.Precise)
        runCurrent()

        assertEquals(DistanceUnit.Miles, preferences.distanceUnit.value)
        assertEquals(LocationPrecision.Precise, settingsRepository.settings.first().locationPrecision)
    }

    @Test
    fun dismissingADialogKeepsSettings() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onOpenDialog(SettingsDialog.LogOut)
        viewModel.onDismissDialog()
        runCurrent()

        assertNull(viewModel.uiState.value.openDialog)
        assertNull(viewModel.uiState.value.exit)
    }

    @Test
    fun logOutSignsOut() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithGoogle("token", "nonce")
        val viewModel = createViewModel()

        viewModel.onLogOutConfirmed()
        runCurrent()

        assertEquals(AuthState.SignedOut, authRepository.authState.value)
        assertEquals(SettingsExit.SignedOut, viewModel.uiState.value.exit)
    }

    @Test
    fun disconnectEndsCoupleAndLocationSharing() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.join("AB12-CD34")
        val viewModel = createViewModel()

        viewModel.onDisconnectConfirmed()
        runCurrent()

        assertNull(coupleRepository.couple.value)
        assertFalse(viewModel.uiState.value.isConnected)
        assertEquals(false, viewModel.uiState.value.settings?.shareLocation)
        assertEquals(SettingsExit.Disconnected, viewModel.uiState.value.exit)
    }

    @Test
    fun turningSharingOffIsSavedAndFailuresAreShown() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        locationRepository.failNextWith = DataError.Network
        viewModel.onShareLocationChange(false)
        runCurrent()
        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertEquals(true, viewModel.uiState.value.settings?.shareLocation)

        viewModel.onShareLocationChange(false)
        runCurrent()
        assertNull(viewModel.uiState.value.error)
        assertEquals(false, viewModel.uiState.value.settings?.shareLocation)
    }

    @Test
    fun deleteAccountSignsOut() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithGoogle("token", "nonce")
        val viewModel = createViewModel()

        viewModel.onDeleteAccountConfirmed()
        runCurrent()

        assertEquals(AuthState.SignedOut, authRepository.authState.value)
        assertEquals(SettingsExit.SignedOut, viewModel.uiState.value.exit)
    }

    @Test
    fun failedDisconnectStaysAndShowsTheError() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.join("AB12-CD34")
        coupleRepository.failNextWith = DataError.Network
        val viewModel = createViewModel()

        viewModel.onDisconnectConfirmed()
        runCurrent()

        assertTrue(viewModel.uiState.value.isConnected)
        assertEquals(true, viewModel.uiState.value.settings?.shareLocation)
        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.exit)
    }

    @Test
    fun failedAccountDeletionShowsTheError() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithGoogle("token", "nonce")
        authRepository.failNextWith = DataError.Unknown
        val viewModel = createViewModel()

        viewModel.onDeleteAccountConfirmed()
        runCurrent()

        assertEquals(DataError.Unknown, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.exit)
    }
}
