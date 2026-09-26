package app.twoverse.core.designsystem.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.WeakHashMap

/** How many composables currently want each window secured. */
private val secureRequests = WeakHashMap<Window, Int>()

/**
 * Blocks screenshots and screen recording while this is in composition (FLAG_SECURE, FR-VLT-6).
 * Requests are counted per window, so moving between two secure screens never clears the flag
 * in between; it is cleared when the last secure screen leaves.
 */
@Composable
fun SecureWindowEffect() {
    val window = LocalContext.current.findActivity()?.window ?: return
    DisposableEffect(window) {
        val count = secureRequests[window] ?: 0
        if (count == 0) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        secureRequests[window] = count + 1
        onDispose {
            val remaining = (secureRequests[window] ?: 1) - 1
            if (remaining <= 0) {
                secureRequests.remove(window)
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                secureRequests[window] = remaining
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
