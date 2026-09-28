package app.twoverse.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.PushRepository
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.CoupleStatus
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val coupleRepository: CoupleRepository,
    private val authRepository: AuthRepository,
    private val pushRepository: PushRepository,
    private val clock: Clock,
) : ViewModel() {

    private val interaction = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        interaction,
        settingsRepository.settings,
        coupleRepository.couple,
    ) { state, settings, couple ->
        state.copy(
            settings = settings,
            isConnected = couple?.status == CoupleStatus.Active,
            connectedSince = couple?.connectedAt?.atZone(clock.zone)?.toLocalDate(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = SettingsUiState(),
    )

    /** Turning sharing off; turning it on goes through the location setup flow. */
    fun onShareLocationChange(enabled: Boolean) {
        viewModelScope.launch { showFailure(settingsRepository.setShareLocation(enabled)) }
    }

    fun onLockOursChange(enabled: Boolean) {
        viewModelScope.launch { showFailure(settingsRepository.setLockOurs(enabled)) }
    }

    fun onOpenDialog(dialog: SettingsDialog) {
        interaction.update { it.copy(openDialog = dialog) }
    }

    fun onDismissDialog() {
        interaction.update { it.copy(openDialog = null) }
    }

    fun onLocationPrecisionSelected(precision: LocationPrecision) {
        closeDialogThen { showFailure(settingsRepository.setLocationPrecision(precision)) }
    }

    fun onDistanceUnitSelected(unit: DistanceUnit) {
        closeDialogThen { showFailure(settingsRepository.setDistanceUnit(unit)) }
    }

    fun onAppearanceSelected(appearance: AppearanceMode) {
        closeDialogThen { showFailure(settingsRepository.setAppearance(appearance)) }
    }

    /** Removes this device's push token first, while still signed in, so pushes stop here. */
    fun onLogOutConfirmed() {
        closeDialogThen {
            pushRepository.unregisterThisDevice()
            exitOnSuccess(authRepository.signOut(), SettingsExit.SignedOut)
        }
    }

    /** The server ends location sharing and hides shared data (BR-9, FR-PAIR-7). */
    fun onDisconnectConfirmed() {
        closeDialogThen {
            val result = coupleRepository.disconnect()
            if (result is DataResult.Success) settingsRepository.setShareLocation(false)
            exitOnSuccess(result, SettingsExit.Disconnected)
        }
    }

    fun onDeleteAccountConfirmed() {
        closeDialogThen { exitOnSuccess(authRepository.deleteAccount(), SettingsExit.SignedOut) }
    }

    private fun showFailure(result: DataResult<Unit>) {
        interaction.update { it.copy(error = (result as? DataResult.Failure)?.error) }
    }

    private fun exitOnSuccess(result: DataResult<Unit>, exit: SettingsExit) {
        when (result) {
            is DataResult.Success -> interaction.update { it.copy(exit = exit) }
            is DataResult.Failure -> interaction.update { it.copy(error = result.error) }
        }
    }

    private fun closeDialogThen(action: suspend () -> Unit) {
        interaction.update { it.copy(openDialog = null, error = null) }
        viewModelScope.launch { action() }
    }

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}
