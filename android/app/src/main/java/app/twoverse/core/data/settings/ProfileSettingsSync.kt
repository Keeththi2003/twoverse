package app.twoverse.core.data.settings

import app.twoverse.core.data.AuthRepository
import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.data.local.UserPreferences
import app.twoverse.core.model.AuthState
import app.twoverse.core.model.DataResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps this device's copy of the settings saved on the profile in step with the server:
 * Lock Ours, distance unit and appearance (FR-VLT-5, FR-SET-3). After a log out Lock Ours
 * returns to on, so the next person to sign in starts with Ours locked.
 */
@Singleton
class ProfileSettingsSync @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val preferences: UserPreferences,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    fun start() {
        appScope.launch {
            authRepository.authState.distinctUntilChanged().collect { state ->
                when (state) {
                    is AuthState.SignedIn -> {
                        val saved = profileRepository.settings()
                        if (saved is DataResult.Success) {
                            preferences.setLockOurs(saved.value.lockOurs)
                            preferences.setDistanceUnit(saved.value.distanceUnit)
                            preferences.setAppearance(saved.value.appearance)
                        }
                    }
                    AuthState.SignedOut -> preferences.setLockOurs(true)
                    AuthState.Loading -> Unit
                }
            }
        }
    }
}
