package app.twoverse.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val TrackWidth = 52.dp
private val TrackHeight = 32.dp
private val ThumbSize = 24.dp
private val ThumbInset = 4.dp
private const val UncheckedTrackAlpha = 0.5f

/**
 * 52 × 32 switch with a primary track and white thumb.
 * Pass `onCheckedChange = null` when a parent row handles the toggle (e.g. [SettingsSwitchRow]).
 */
@Composable
fun TwoverseSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = TwoverseTheme.colors
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.primary else colors.onSurfaceVariant.copy(alpha = UncheckedTrackAlpha),
        label = "trackColor",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) TrackWidth - ThumbSize - ThumbInset * 2 else 0.dp,
        label = "thumbOffset",
    )
    val toggleModifier = if (onCheckedChange != null) {
        Modifier
            .minimumInteractiveComponentSize()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(toggleModifier)
            .size(width = TrackWidth, height = TrackHeight)
            .background(trackColor, TwoverseTheme.shapes.circle)
            .padding(ThumbInset),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(x = thumbOffset.roundToPx(), y = 0) }
                .size(ThumbSize)
                .background(Color.White, TwoverseTheme.shapes.circle),
        )
    }
}

@PreviewLightDark
@Composable
private fun TwoverseSwitchPreview() {
    TwoversePreviewBackground {
        Row(horizontalArrangement = Arrangement.spacedBy(TwoverseTheme.spacing.md)) {
            TwoverseSwitch(checked = true, onCheckedChange = {})
            TwoverseSwitch(checked = false, onCheckedChange = {})
        }
    }
}
