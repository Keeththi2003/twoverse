package app.twoverse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.twoverse.core.designsystem.theme.TwoverseTheme

/** Wraps previews in the theme on the screen background colour. */
@Composable
internal fun TwoversePreviewBackground(content: @Composable () -> Unit) {
    TwoverseTheme {
        Box(
            modifier = Modifier
                .background(TwoverseTheme.colors.background)
                .padding(TwoverseTheme.spacing.md),
        ) {
            content()
        }
    }
}
