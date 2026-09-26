package app.twoverse.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.CoupleStatus
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

    fun onShareLocationChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShareLocation(enabled) }
    }

    fun onLockOursChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLockOurs(enabled) }
    }

    fun onOpenDialog(dialog: SettingsDialog) {
        interaction.update { it.copy(openDialog = dialog) }
    }

    fun onDismissDialog() {
        interaction.update { it.copy(openDialog = null) }
    }

    fun onLocationPrecisionSelected(precision: LocationPrecision) {
        closeDialogThen { settingsRepository.setLocationPrecision(precision) }
    }

    fun onDistanceUnitSelected(unit: DistanceUnit) {
        closeDialogThen { settingsRepository.setDistanceUnit(unit) }
    }

    fun onAppearanceSelected(appearance: AppearanceMode) {
        closeDialogThen { settingsRepository.setAppearance(appearance) }
    }

    fun onLogOutConfirmed() {
        closeDialogThen {
            authRepository.signOut()
            interaction.update { it.copy(exit = SettingsExit.SignedOut) }
        }
    }

    /** Disconnecting ends location sharing immediately (BR-9, FR-PAIR-7). */
    fun onDisconnectConfirmed() {
        closeDialogThen {
            settingsRepository.setShareLocation(false)
            coupleRepository.disconnect()
            interaction.update { it.copy(exit = SettingsExit.Disconnected) }
        }
    }

    fun onDeleteAccountConfirmed() {
        closeDialogThen {
            authRepository.requestAccountDeletion()
            interaction.update { it.copy(exit = SettingsExit.SignedOut) }
        }
    }

    private fun closeDialogThen(action: suspend () -> Unit) {
        interaction.update { it.copy(openDialog = null) }
        viewModelScope.launch { action() }
    }

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}
