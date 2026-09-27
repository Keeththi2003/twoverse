package app.twoverse.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.twoverse.R
import app.twoverse.core.designsystem.component.TwoverseBackButton
import app.twoverse.core.designsystem.component.TwoversePasswordField
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.component.TwoverseTextField
import app.twoverse.core.designsystem.text.messageRes
import app.twoverse.core.designsystem.theme.TwoverseTheme
import app.twoverse.core.model.DataError

/** Create an account (FR-AUTH-2, FR-AUTH-5). */
@Composable
fun SignUpScreen(
    uiState: SignUpUiState,
    onBack: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFormLayout(
        title = stringResource(R.string.sign_up_title),
        subtitle = stringResource(R.string.sign_up_subtitle),
        modifier = modifier,
        header = { TwoverseBackButton(onClick = onBack) },
        top = {
            TwoverseTextField(
                value = uiState.displayName,
                onValueChange = onDisplayNameChange,
                label = stringResource(R.string.sign_up_name_label),
                placeholder = stringResource(R.string.sign_up_name_placeholder),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                errorText = if (uiState.problem == SignUpProblem.NameMissing) stringResource(R.string.sign_up_name_missing) else null,
            )
            TwoverseTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.sign_in_email_label),
                placeholder = stringResource(R.string.sign_in_email_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                errorText = if (uiState.problem == SignUpProblem.EmailInvalid) stringResource(R.string.sign_up_email_invalid) else null,
            )
            TwoversePasswordField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.sign_in_password_label),
                placeholder = stringResource(R.string.sign_up_password_placeholder),
                isVisible = uiState.isPasswordVisible,
                onToggleVisibility = onTogglePasswordVisibility,
                onImeAction = onSignUp,
            )
            when {
                uiState.error != null -> FormMessage(stringResource(uiState.error.messageRes()), isError = true)
                uiState.isConfirmationSent -> FormMessage(stringResource(R.string.sign_up_confirm_email), isError = false)
            }
        },
        bottom = {
            TwoversePrimaryButton(
                text = stringResource(R.string.sign_up_button),
                onClick = onSignUp,
                enabled = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TwoverseTheme.spacing.xs),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sign_up_have_account),
                    style = MaterialTheme.typography.labelLarge,
                    color = TwoverseTheme.colors.onSurfaceVariant,
                )
                TwoverseTextButton(text = stringResource(R.string.sign_in_button), onClick = onBack)
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun SignUpScreenPreview() {
    TwoverseTheme {
        SignUpScreen(
            uiState = SignUpUiState(displayName = "Keeththi", email = "you@example.com", error = DataError.EmailInUse),
            onBack = {},
            onDisplayNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onSignUp = {},
        )
    }
}
