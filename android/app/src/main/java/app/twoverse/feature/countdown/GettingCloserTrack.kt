package app.twoverse.feature.countdown

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.twoverse.core.designsystem.component.Planet
import app.twoverse.core.designsystem.component.PlanetKind
import app.twoverse.core.designsystem.theme.TwoverseTheme

private val TrackHeight = 40.dp
/** The line starts at the centre of your planet and ends at the centre of hers. */
private val TrackStart = 16.dp
private val TrackEndInset = 20.dp
private val TrackStroke = 2.dp
private val HerSize = 40.dp
private val YouSize = 30.dp
private val YouTop = 5.dp

/** Line from your planet (left) to hers (right), filled up to [progress] (0..1), as on Home. */
@Composable
internal fun GettingCloserTrack(progress: Float, modifier: Modifier = Modifier) {
    val colors = TwoverseTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TrackHeight)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height / 2
            val start = TrackStart.toPx()
            val end = size.width - TrackEndInset.toPx()
            drawLine(colors.outline, Offset(start, y), Offset(end, y), strokeWidth = TrackStroke.toPx())
            drawLine(
                color = colors.primary,
                start = Offset(start, y),
                end = Offset(start + (end - start) * progress, y),
                strokeWidth = TrackStroke.toPx(),
            )
        }
        Planet(
            kind = PlanetKind.You,
            size = YouSize,
            modifier = Modifier.offset(y = YouTop),
        )
        Planet(
            kind = PlanetKind.Her,
            size = HerSize,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}
