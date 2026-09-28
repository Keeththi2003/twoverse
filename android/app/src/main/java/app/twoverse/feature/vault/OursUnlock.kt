package app.twoverse.feature.vault

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.twoverse.R

private const val Authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/**
 * Returns an action that asks for the fingerprint, face or device PIN, pattern or password and
 * calls [onUnlocked] once the user confirms (FR-VLT-5). A phone without any screen lock can't
 * confirm anything, so Ours opens directly there.
 */
@Composable
internal fun rememberOursUnlocker(onUnlocked: () -> Unit): () -> Unit {
    val activity = LocalActivity.current as? FragmentActivity
    val currentOnUnlocked by rememberUpdatedState(onUnlocked)
    val title = stringResource(R.string.vault_unlock_prompt_title)
    val subtitle = stringResource(R.string.vault_unlock_prompt_subtitle)
    return remember(activity, title, subtitle) {
        unlock@{
            if (activity == null) return@unlock
            if (BiometricManager.from(activity).canAuthenticate(Authenticators) == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                currentOnUnlocked()
                return@unlock
            }
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    currentOnUnlocked()
                }
            }
            BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(Authenticators)
                    .build(),
            )
        }
    }
}
