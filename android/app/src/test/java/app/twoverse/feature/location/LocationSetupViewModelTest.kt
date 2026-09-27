package app.twoverse.feature.location

import app.twoverse.core.data.fake.FakeLocationPermissionChecker
import app.twoverse.core.data.fake.FakeLocationRepository
import app.twoverse.core.data.settings.DefaultSettingsRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.LocationPermissionStatus
import app.twoverse.testing.InMemoryUserPreferences
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
class LocationSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val none = LocationPermissionStatus(foreground = false, background = false)
    private val whileUsing = LocationPermissionStatus(foreground = true, background = false)
    private val all = LocationPermissionStatus(foreground = true, background = true)

    private val locationRepository = FakeLocationRepository()
    private val preferences = InMemoryUserPreferences()
    private val permissions = FakeLocationPermissionChecker(none)
    private val viewModel = LocationSetupViewModel(
        DefaultSettingsRepository(locationRepository, preferences),
        preferences,
        permissions,
    )

    private suspend fun startWithSharingOff() {
        locationRepository.setSharingEnabled(false)
        viewModel.onPermissionsChanged()
    }

    @Test
    fun explainsBeforeAskingForPermission() = runTest(mainDispatcherRule.testDispatcher) {
        startWithSharingOff()

        assertEquals(LocationSetupStep.Explain, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.permissionRequest)

        viewModel.onContinue()
        assertEquals(PermissionRequest.Foreground, viewModel.uiState.value.permissionRequest)
    }

    @Test
    fun deniedCanBeAskedAgainOrNeedsSettings() = runTest(mainDispatcherRule.testDispatcher) {
        startWithSharingOff()

        viewModel.onForegroundResult(canAskAgain = true)
        assertEquals(LocationSetupStep.Denied, viewModel.uiState.value.step)

        viewModel.onForegroundResult(canAskAgain = false)
        assertEquals(LocationSetupStep.DeniedForever, viewModel.uiState.value.step)
    }

    @Test
    fun allowingInSystemSettingsContinues() = runTest(mainDispatcherRule.testDispatcher) {
        startWithSharingOff()
        viewModel.onForegroundResult(canAskAgain = false)

        permissions.current = whileUsing
        viewModel.onPermissionsChanged()

        assertEquals(LocationSetupStep.Background, viewModel.uiState.value.step)
    }

    @Test
    fun whileUsingPermissionOffersAllTheTimeThenEnablesSharing() = runTest(mainDispatcherRule.testDispatcher) {
        startWithSharingOff()
        permissions.current = whileUsing
        viewModel.onForegroundResult(canAskAgain = true)
        assertEquals(LocationSetupStep.Background, viewModel.uiState.value.step)

        viewModel.onAllowAllTheTime()
        assertEquals(PermissionRequest.Background, viewModel.uiState.value.permissionRequest)
        viewModel.onPermissionRequestShown()
        viewModel.onBackgroundResult()
        runCurrent()

        assertTrue(locationRepository.sharing.value.enabled)
        assertEquals(LocationSetupStep.BatteryGuide, viewModel.uiState.value.step)
    }

    @Test
    fun onlyWhileUsingStillEnablesSharing() = runTest(mainDispatcherRule.testDispatcher) {
        startWithSharingOff()
        permissions.current = whileUsing
        viewModel.onForegroundResult(canAskAgain = true)

        viewModel.onWhileUsingOnly()
        runCurrent()

        assertTrue(locationRepository.sharing.value.enabled)
    }

    @Test
    fun batteryGuideIsShownOnlyOnce() = runTest(mainDispatcherRule.testDispatcher) {
        permissions.current = all
        startWithSharingOff()
        runCurrent()
        assertEquals(LocationSetupStep.BatteryGuide, viewModel.uiState.value.step)

        viewModel.onBatteryGuideDone()
        runCurrent()
        assertTrue(preferences.batteryGuideShown.value)
        assertTrue(viewModel.uiState.value.isFinished)

        val again = LocationSetupViewModel(DefaultSettingsRepository(locationRepository, preferences), preferences, permissions)
        again.onPermissionsChanged()
        runCurrent()
        assertTrue(again.uiState.value.isFinished)
    }

    @Test
    fun failedEnableCanBeRetried() = runTest(mainDispatcherRule.testDispatcher) {
        permissions.current = all
        locationRepository.setSharingEnabled(false)
        locationRepository.failNextWith = DataError.Network
        viewModel.onPermissionsChanged()
        runCurrent()

        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertFalse(locationRepository.sharing.value.enabled)

        viewModel.onRetry()
        runCurrent()
        assertTrue(locationRepository.sharing.value.enabled)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun notNowLeavesSharingOff() = runTest(mainDispatcherRule.testDispatcher) {
        startWithSharingOff()

        viewModel.onNotNow()

        assertTrue(viewModel.uiState.value.isFinished)
        assertFalse(locationRepository.sharing.value.enabled)
    }
}
