package app.twoverse.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twoverse.BuildConfig
import kotlinx.coroutines.launch

@Composable
fun SignInRoute(
    onSignedIn: (SignedInDestination) -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSignedIn by rememberUpdatedState(onSignedIn)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.destination) {
        uiState.destination?.let { currentOnSignedIn(it) }
    }

    SignInScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onGoogleSignIn = {
            scope.launch {
                when (val result = requestGoogleIdToken(context, BuildConfig.GOOGLE_WEB_CLIENT_ID)) {
                    is GoogleSignInResult.Token -> viewModel.onGoogleIdToken(result.idToken, result.rawNonce)
                    GoogleSignInResult.Failed -> viewModel.onGoogleSignInFailed()
                    GoogleSignInResult.Cancelled -> Unit
                }
            }
        },
        onSignIn = viewModel::onEmailSignIn,
        onForgotPassword = viewModel::onForgotPassword,
        onCreateAccount = onCreateAccount,
        modifier = modifier,
    )
}
