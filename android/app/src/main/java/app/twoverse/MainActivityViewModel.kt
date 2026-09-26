package app.twoverse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.network.NetworkMonitor
import app.twoverse.core.model.AppearanceMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-wide state the activity needs: the chosen appearance (FR-SET-3) and connectivity (NFR-REL-1). */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    val appearance: StateFlow<AppearanceMode> = settingsRepository.settings
        .map { it.appearance }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceMode.System)

    val isOffline: StateFlow<Boolean> = networkMonitor.isOnline
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), false)

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}
