package app.twoverse.feature.compass

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CompassRoute(
    modifier: Modifier = Modifier,
    viewModel: CompassViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val needleRotation by viewModel.needleRotation.collectAsStateWithLifecycle()
    val view = LocalView.current
    val pointing = (uiState as? CompassUiState.Success)?.isPointingAtPartner == true

    // A short tap when the phone turns to face the partner (FR-CMP-7).
    LaunchedEffect(pointing) {
        if (!pointing) return@LaunchedEffect
        val feedback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        view.performHapticFeedback(feedback)
    }

    CompassScreen(uiState = uiState, needleRotation = { needleRotation }, modifier = modifier)
}
