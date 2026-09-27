package app.twoverse.feature.location

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LocationSetupRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    val context = LocalContext.current

    val foregroundRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val canAskAgain = (context as? Activity)?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION)
        } ?: true
        viewModel.onForegroundResult(canAskAgain)
    }
    val backgroundRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onBackgroundResult()
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onPermissionsChanged()
        onPauseOrDispose {}
    }
    LaunchedEffect(uiState.permissionRequest) {
        when (uiState.permissionRequest) {
            PermissionRequest.Foreground -> foregroundRequest.launch(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            )
            PermissionRequest.Background -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                viewModel.onBackgroundResult()
            }
            null -> Unit
        }
        if (uiState.permissionRequest != null) viewModel.onPermissionRequestShown()
    }
    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) currentOnDone()
    }

    val openAppSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        )
    }
    LocationSetupScreen(
        uiState = uiState,
        actions = LocationSetupActions(
            onContinue = viewModel::onContinue,
            onNotNow = viewModel::onNotNow,
            onOpenSettings = openAppSettings,
            onAllowAllTheTime = viewModel::onAllowAllTheTime,
            onWhileUsingOnly = viewModel::onWhileUsingOnly,
            onRetry = viewModel::onRetry,
            onOpenBatterySettings = openAppSettings,
            onBatteryGuideDone = viewModel::onBatteryGuideDone,
        ),
        modifier = modifier,
    )
}
