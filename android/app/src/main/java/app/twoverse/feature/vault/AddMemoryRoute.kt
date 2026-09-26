package app.twoverse.feature.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twoverse.core.designsystem.component.SecureWindowEffect

@Composable
fun AddMemoryRoute(
    onClose: () -> Unit,
    onSent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddMemoryViewModel = hiltViewModel(),
) {
    SecureWindowEffect()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSent by rememberUpdatedState(onSent)
    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let { viewModel.onPhotoPicked(it.toString()) }
    }

    LaunchedEffect(uiState.isSent) {
        if (uiState.isSent) currentOnSent()
    }

    AddMemoryScreen(
        uiState = uiState,
        onClose = onClose,
        onPickPhoto = { photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
        onCaptionChange = viewModel::onCaptionChange,
        onExpirySelected = viewModel::onExpirySelected,
        onAllowKeepChange = viewModel::onAllowKeepChange,
        onSend = viewModel::onSend,
        modifier = modifier,
    )
}
