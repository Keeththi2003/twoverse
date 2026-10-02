package app.twoverse.feature.star

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.twoverse.core.designsystem.component.SecureWindowEffect

/** Shows waiting Shooting Stars, or one again; [onDone] continues into the app. */
@Composable
fun ShootingStarRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShootingStarViewModel = hiltViewModel(),
) {
    SecureWindowEffect()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)

    LaunchedEffect(uiState) {
        if (uiState == ShootingStarUiState.Done) currentOnDone()
    }

    ShootingStarScreen(uiState = uiState, onEnter = viewModel::onEnter, modifier = modifier)
}
