package app.twoverse.feature.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.LocationPermissionChecker
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.local.UserPreferences
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.LocationPermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Walks the user through permissions and turns location sharing on (FR-LOC-1, FR-LOC-2). */
@HiltViewModel
class LocationSetupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val preferences: UserPreferences,
    private val permissions: LocationPermissionChecker,
) : ViewModel() {

    private val state = MutableStateFlow(LocationSetupUiState())
    val uiState: StateFlow<LocationSetupUiState> = state.asStateFlow()

    /** Called when the screen appears or returns from system settings. */
    fun onPermissionsChanged() {
        val step = state.value.step
        if (step != LocationSetupStep.Checking && step != LocationSetupStep.DeniedForever) return
        val status = permissions.status()
        if (status.foreground) continueWith(status) else if (step == LocationSetupStep.Checking) setStep(LocationSetupStep.Explain)
    }

    fun onContinue() {
        state.update { it.copy(permissionRequest = PermissionRequest.Foreground) }
    }

    fun onPermissionRequestShown() {
        state.update { it.copy(permissionRequest = null) }
    }

    /** [canAskAgain] is false when Android will no longer show the dialog. */
    fun onForegroundResult(canAskAgain: Boolean) {
        val status = permissions.status()
        when {
            status.foreground -> continueWith(status)
            canAskAgain -> setStep(LocationSetupStep.Denied)
            else -> setStep(LocationSetupStep.DeniedForever)
        }
    }

    fun onAllowAllTheTime() {
        state.update { it.copy(permissionRequest = PermissionRequest.Background) }
    }

    /** Sharing works either way; without background permission it only updates while the app is open. */
    fun onBackgroundResult() {
        enableSharing()
    }

    fun onWhileUsingOnly() {
        enableSharing()
    }

    fun onRetry() {
        enableSharing()
    }

    fun onBatteryGuideDone() {
        viewModelScope.launch {
            preferences.setBatteryGuideShown()
            state.update { it.copy(isFinished = true) }
        }
    }

    fun onNotNow() {
        state.update { it.copy(isFinished = true) }
    }

    private fun continueWith(status: LocationPermissionStatus) {
        if (status.background) enableSharing() else setStep(LocationSetupStep.Background)
    }

    private fun enableSharing() {
        state.update { it.copy(step = LocationSetupStep.Enabling, error = null) }
        viewModelScope.launch {
            when (val result = settingsRepository.setShareLocation(true)) {
                is DataResult.Success -> {
                    if (preferences.batteryGuideShown.first()) {
                        state.update { it.copy(isFinished = true) }
                    } else {
                        setStep(LocationSetupStep.BatteryGuide)
                    }
                }
                is DataResult.Failure -> state.update { it.copy(error = result.error) }
            }
        }
    }

    private fun setStep(step: LocationSetupStep) {
        state.update { it.copy(step = step) }
    }
}
