package app.twoverse.feature.settings

import app.twoverse.core.data.fake.FakeAuthRepository
import app.twoverse.core.data.fake.FakeCoupleRepository
import app.twoverse.core.data.fake.FakeLocationRepository
import app.twoverse.core.data.fake.FakeProfileRepository
import app.twoverse.core.data.fake.FakePushRepository
import app.twoverse.core.data.sample.SampleData
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
    private val profileRepository = FakeProfileRepository()
    private val settingsRepository = DefaultSettingsRepository(locationRepository, profileRepository, preferences)
    private val coupleRepository = FakeCoupleRepository()
    private val authRepository = FakeAuthRepository()
    private val pushRepository = FakePushRepository()
    private val clock = Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneId.of("Asia/Colombo"))

    private fun TestScope.createViewModel(): SettingsViewModel {
        val viewModel = SettingsViewModel(settingsRepository, coupleRepository, authRepository, pushRepository, profileRepository, clock)
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
        assertFalse(profileRepository.serverSettings.lockOurs)
    }

    @Test
    fun lockOursStaysOnWhenSavingFails() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        profileRepository.failNextWith(DataError.Network)

        viewModel.onLockOursChange(false)
        runCurrent()

        assertEquals(true, viewModel.uiState.value.settings?.lockOurs)
        assertEquals(DataError.Network, viewModel.uiState.value.error)
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
        assertEquals(AppearanceMode.Dark, profileRepository.serverSettings.appearance)
    }

    @Test
    fun aDistanceUnitThatCannotBeSavedIsNotChanged() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        profileRepository.failNextWith(DataError.Network)

        viewModel.onDistanceUnitSelected(DistanceUnit.Miles)
        runCurrent()

        assertEquals(DistanceUnit.Kilometres, preferences.distanceUnit.value)
        assertEquals(DataError.Network, viewModel.uiState.value.error)
    }

    @Test
    fun showsThePartnerAndTheirDisplayName() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        assertNull(viewModel.uiState.value.partner)
        assertNull(viewModel.uiState.value.partnerDisplayName)

        coupleRepository.setCouple(SampleData.couple)
        runCurrent()
        assertEquals(SampleData.partner, viewModel.uiState.value.partner)
        assertEquals("Ammu", viewModel.uiState.value.partnerDisplayName)

        coupleRepository.setCouple(SampleData.couple.copy(partner = SampleData.partner.copy(nickname = "Chellam")))
        runCurrent()
        assertEquals("Chellam", viewModel.uiState.value.partnerDisplayName)
    }

    @Test
    fun theNicknameDialogStartsFromTheCurrentNickname() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.setCouple(SampleData.couple.copy(partner = SampleData.partner.copy(nickname = "Chellam")))
        val viewModel = createViewModel()

        viewModel.onOpenDialog(SettingsDialog.Nickname)
        runCurrent()

        assertEquals(SettingsDialog.Nickname, viewModel.uiState.value.openDialog)
        assertEquals("Chellam", viewModel.uiState.value.nicknameDraft)
    }

    @Test
    fun savingANicknameTrimsItCapsItAndRefreshesTheCouple() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.setCouple(SampleData.couple)
        val viewModel = createViewModel()
        viewModel.onOpenDialog(SettingsDialog.Nickname)

        viewModel.onNicknameChange("x".repeat(40))
        runCurrent()
        assertEquals(30, viewModel.uiState.value.nicknameDraft.length)
        viewModel.onNicknameChange("  Kanna  ")
        viewModel.onSaveNickname()
        runCurrent()

        assertEquals(listOf<String?>("Kanna"), profileRepository.savedNicknames)
        assertEquals(1, coupleRepository.refreshCount)
        assertNull(viewModel.uiState.value.openDialog)
    }

    @Test
    fun aBlankOrClearedNicknameRemovesIt() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.setCouple(SampleData.couple)
        val viewModel = createViewModel()

        viewModel.onOpenDialog(SettingsDialog.Nickname)
        viewModel.onNicknameChange("   ")
        viewModel.onSaveNickname()
        viewModel.onOpenDialog(SettingsDialog.Nickname)
        viewModel.onClearNickname()
        runCurrent()

        assertEquals(listOf<String?>(null, null), profileRepository.savedNicknames)
    }

    @Test
    fun aFailedNicknameShowsTheErrorAndKeepsTheCouple() = runTest(mainDispatcherRule.testDispatcher) {
        coupleRepository.setCouple(SampleData.couple)
        val viewModel = createViewModel()
        profileRepository.failNextWith(DataError.Network)

        viewModel.onOpenDialog(SettingsDialog.Nickname)
        viewModel.onNicknameChange("Kanna")
        viewModel.onSaveNickname()
        runCurrent()

        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertEquals(0, coupleRepository.refreshCount)
    }

    @Test
    fun distanceUnitAndPrecisionAreSaved() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onDistanceUnitSelected(DistanceUnit.Miles)
        viewModel.onLocationPrecisionSelected(LocationPrecision.Precise)
        runCurrent()

        assertEquals(DistanceUnit.Miles, preferences.distanceUnit.value)
        assertEquals(DistanceUnit.Miles, profileRepository.serverSettings.distanceUnit)
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
        assertEquals(listOf("unregister"), pushRepository.calls)
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
