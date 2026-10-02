package app.twoverse.feature.star

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

/** "Send a Shooting Star", or edit one when the route carries a star id; [onDone] returns. */
@Composable
fun StarComposerRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StarComposerViewModel = hiltViewModel(),
) {
    SecureWindowEffect()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let { viewModel.onPhotoPicked(it.toString()) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnDone()
    }

    StarComposerScreen(
        uiState = uiState,
        actions = StarComposerActions(
            onBack = onDone,
            onRetryLoad = viewModel::load,
            onLayoutSelected = viewModel::onLayoutSelected,
            onTemplateSelected = viewModel::onTemplateSelected,
            onEyebrowChange = viewModel::onEyebrowChange,
            onTitleChange = viewModel::onTitleChange,
            onMessageChange = viewModel::onMessageChange,
            onSignatureChange = viewModel::onSignatureChange,
            onPickPhoto = { photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
            onRemovePhoto = viewModel::onRemovePhoto,
            onPhotoFitSelected = viewModel::onPhotoFitSelected,
            onShowNextOpen = viewModel::onShowNextOpen,
            onOpenPicker = viewModel::onOpenPicker,
            onDismissPicker = viewModel::onDismissPicker,
            onDateSelected = viewModel::onDateSelected,
            onTimeSelected = viewModel::onTimeSelected,
            onOpenPreview = viewModel::onOpenPreview,
            onPreviewDarkChange = viewModel::onPreviewDarkChange,
            onClosePreview = viewModel::onClosePreview,
            onSend = viewModel::onSend,
        ),
        modifier = modifier,
    )
}
