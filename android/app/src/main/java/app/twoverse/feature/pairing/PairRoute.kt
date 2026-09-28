package app.twoverse.feature.pairing

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twoverse.R
import kotlinx.coroutines.launch

@Composable
fun PairRoute(
    onBack: (() -> Unit)?,
    onConnected: (showBirthday: Boolean) -> Unit,
    onReconnect: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PairViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnConnected by rememberUpdatedState(onConnected)
    val currentOnSignedOut by rememberUpdatedState(onSignedOut)
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val shareMessage = uiState.coupleCode?.let { stringResource(R.string.pair_share_message, it) }

    LaunchedEffect(uiState.isConnected) {
        if (uiState.isConnected) currentOnConnected(uiState.hasBirthdayWelcome)
    }
    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) currentOnSignedOut()
    }

    PairScreen(
        uiState = uiState,
        actions = PairActions(
            onBack = onBack,
            onShareCode = {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, shareMessage)
                context.startActivity(Intent.createChooser(send, null))
            },
            onCopyCode = { code ->
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(code, code)))
                }
                // Android 13+ shows its own clipboard confirmation.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(context, R.string.pair_copied, Toast.LENGTH_SHORT).show()
                }
            },
            onRetryCode = viewModel::loadCode,
            onPartnerCodeChange = viewModel::onPartnerCodeChange,
            onConnect = viewModel::onConnect,
            onReconnect = onReconnect,
            onLogOut = viewModel::onLogOut,
            onLogOutConfirmed = viewModel::onLogOutConfirmed,
            onLogOutDismissed = viewModel::onLogOutDismissed,
        ),
        modifier = modifier,
    )
}
