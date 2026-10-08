package app.twoverse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.twoverse.core.common.PartnerName
import app.twoverse.core.common.toPartnerName
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.network.NetworkMonitor
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.LaunchScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * App-wide state the activity needs: the chosen appearance (FR-SET-3), connectivity (NFR-REL-1),
 * the partner's name for every screen and whether to ask About you (FR-PRO-1, FR-PRO-2).
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    networkMonitor: NetworkMonitor,
    coupleRepository: CoupleRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {

    /** The partner as every screen names them: my nickname, else their short name (FR-PRO-1). */
    val partner: StateFlow<PartnerName?> = coupleRepository.couple
        .map { it?.partner?.toPartnerName() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Signed in without pronouns yet: the About you step must be completed (FR-PRO-2). */
    val needsAboutYou: StateFlow<Boolean> = profileRepository.myProfile
        .map { it != null && it.pronouns == null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val appearance: StateFlow<AppearanceMode> = settingsRepository.settings
        .map { it.appearance }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceMode.System)

    private val launchScreen = MutableStateFlow<LaunchScreen?>(null)

    /** A notification or the widget asked to open this screen. */
    val requestedScreen: StateFlow<LaunchScreen?> = launchScreen.asStateFlow()

    private val passwordRecovery = MutableStateFlow(false)

    /** A password-reset link was opened; the app shows Reset password once (FR-AUTH-3). */
    val isPasswordRecovery: StateFlow<Boolean> = passwordRecovery.asStateFlow()

    val isOffline: StateFlow<Boolean> = networkMonitor.isOnline
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), false)

    fun onLaunchScreen(screen: LaunchScreen) {
        launchScreen.value = screen
    }

    fun onLaunchScreenShown() {
        launchScreen.value = null
    }

    fun onPasswordRecovery() {
        passwordRecovery.value = true
    }

    fun onPasswordRecoveryShown() {
        passwordRecovery.value = false
    }

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}
