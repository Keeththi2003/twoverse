package app.twoverse.feature.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.R
import app.twoverse.core.designsystem.component.TwoversePasswordField
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DataError

/** Choose a new password (FR-AUTH-3). */
@Composable
fun ResetPasswordScreen(
    uiState: ResetPasswordUiState,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFormLayout(
        title = stringResource(R.string.reset_password_title),
        subtitle = stringResource(R.string.reset_password_subtitle),
        modifier = modifier,
        top = {
            TwoversePasswordField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.reset_password_label),
                placeholder = stringResource(R.string.sign_up_password_placeholder),
                isVisible = uiState.isPasswordVisible,
                onToggleVisibility = onTogglePasswordVisibility,
                onImeAction = onSave,
            )
            uiState.error?.let { FormMessage(stringResource(it.messageRes()), isError = true) }
        },
        bottom = {
            TwoversePrimaryButton(
                text = stringResource(R.string.reset_password_button),
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@PreviewLightDark
@Composable
private fun ResetPasswordScreenPreview() {
    TwoverseTheme {
        ResetPasswordScreen(
            uiState = ResetPasswordUiState(password = "abc", error = DataError.WeakPassword),
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onSave = {},
        )
    }
}
