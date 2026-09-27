package app.twoverse.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeRoute(
    onOpenCompass: () -> Unit,
    onOpenCountdown: () -> Unit,
    onOpenVault: () -> Unit,
    onSendMemory: () -> Unit,
    onOpenLocationSetup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val shouldAskNotifications by viewModel.shouldAskNotificationPermission.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    // Android 13+ needs permission to show notifications (FR-NOT); asked once, on Home.
    LaunchedEffect(shouldAskNotifications) {
        if (!shouldAskNotifications || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        viewModel.onNotificationPermissionAsked()
    }
    HomeScreen(
        uiState = uiState,
        onOpenCompass = onOpenCompass,
        onOpenCountdown = onOpenCountdown,
        onOpenVault = onOpenVault,
        onSendMemory = onSendMemory,
        onOpenLocationSetup = onOpenLocationSetup,
        modifier = modifier,
    )
}
