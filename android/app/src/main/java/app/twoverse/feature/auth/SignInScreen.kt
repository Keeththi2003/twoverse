package app.twoverse.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.twoverse.R
import app.twoverse.core.designsystem.component.OrDivider
import app.twoverse.core.designsystem.component.OrbitGraphic
import app.twoverse.core.designsystem.component.OrbitPosition
import app.twoverse.core.designsystem.component.TwoverseOutlineButton
import app.twoverse.core.designsystem.component.TwoversePrimaryButton
import app.twoverse.core.designsystem.component.TwoverseTextButton
import app.twoverse.core.designsystem.component.TwoverseTextField
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val LogoSize = 44.dp
private val LogoHerSize = 15.dp
private val LogoYouSize = 11.dp
private val LogoStarSize = 10.dp

/** Sign in (FR-AUTH-1 to FR-AUTH-3). */
@Composable
fun SignInScreen(
    uiState: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwoverseTheme.colors
    val spacing = TwoverseTheme.spacing

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontalWide,
                    end = spacing.screenHorizontalWide,
                    top = spacing.xs,
                    bottom = spacing.sm,
                ),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                SignInLogo()
                Spacer(modifier = Modifier.height(spacing.xl))
                Text(
                    text = stringResource(R.string.sign_in_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = stringResource(R.string.sign_in_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(spacing.xxl))
                TwoverseOutlineButton(
                    text = stringResource(R.string.sign_in_google),
                    onClick = onGoogleSignIn,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OrDivider(modifier = Modifier.padding(vertical = spacing.xl))
                SignInFields(
                    uiState = uiState,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onTogglePasswordVisibility = onTogglePasswordVisibility,
                    onSignIn = onSignIn,
                    onForgotPassword = onForgotPassword,
                )
            }
            Column(modifier = Modifier.padding(top = spacing.xl)) {
                TwoversePrimaryButton(
                    text = stringResource(R.string.sign_in_button),
                    onClick = onSignIn,
                    enabled = uiState.canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.xs),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.sign_in_new_to_twoverse),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                    )
                    TwoverseTextButton(
                        text = stringResource(R.string.sign_in_create_account),
                        onClick = onCreateAccount,
                    )
                }
            }
        }
    }
}

@Composable
private fun SignInLogo() {
    OrbitGraphic(
        size = LogoSize,
        her = OrbitPosition(angleDegrees = 33f),
        you = OrbitPosition(angleDegrees = 229f),
        herSize = LogoHerSize,
        youSize = LogoYouSize,
        starSize = LogoStarSize,
        showInnerRing = false,
        showShadows = false,
    )
}

@Composable
private fun SignInFields(
    uiState: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    val spacing = TwoverseTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        TwoverseTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.sign_in_email_label),
            placeholder = stringResource(R.string.sign_in_email_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        TwoverseTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.sign_in_password_label),
            placeholder = stringResource(R.string.sign_in_password_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSignIn() }),
            visualTransformation = if (uiState.isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        painter = painterResource(
                            if (uiState.isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye,
                        ),
                        contentDescription = stringResource(
                            if (uiState.isPasswordVisible) R.string.sign_in_hide_password else R.string.sign_in_show_password,
                        ),
                        tint = TwoverseTheme.colors.onSurfaceVariant,
                    )
                }
            },
        )
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            TwoverseTextButton(
                text = stringResource(R.string.sign_in_forgot_password),
                onClick = onForgotPassword,
            )
            uiState.message?.let { message ->
                Text(
                    text = stringResource(message.textRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message == SignInMessage.ResetSent) {
                        TwoverseTheme.colors.onSurfaceVariant
                    } else {
                        TwoverseTheme.colors.error
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

private fun SignInMessage.textRes(): Int = when (this) {
    SignInMessage.ResetSent -> R.string.sign_in_reset_sent
    SignInMessage.ResetNeedsEmail -> R.string.sign_in_reset_needs_email
    SignInMessage.SignInFailed -> R.string.sign_in_error
}

@PreviewLightDark
@Composable
private fun SignInScreenPreview() {
    TwoverseTheme {
        SignInScreen(
            uiState = SignInUiState(email = "you@example.com"),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onGoogleSignIn = {},
            onSignIn = {},
            onForgotPassword = {},
            onCreateAccount = {},
        )
    }
}
