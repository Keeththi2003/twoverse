package app.twoverse.core.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.hypot
import kotlin.math.max

/** Colour stops for the planet and star radial gradients (DESIGN.md §2). */
data class GradientStops(
    val highlight: Color,
    val body: Color,
    val bodyStop: Float,
    val edge: Color,
    val centerX: Float,
    val centerY: Float,
)

object TwoverseGradients {
    val Rose = GradientStops(
        highlight = Color(0xFFFFE0E8),
        body = Color(0xFFE0607E),
        bodyStop = 0.55f,
        edge = Color(0xFF9E3552),
        centerX = 0.32f,
        centerY = 0.30f,
    )
    val Lavender = GradientStops(
        highlight = Color(0xFFF1E9FF),
        body = Color(0xFF9D86D0),
        bodyStop = 0.55f,
        edge = Color(0xFF5E4A94),
        centerX = 0.32f,
        centerY = 0.30f,
    )
    val Gold = GradientStops(
        highlight = Color(0xFFFFF3D1),
        body = Color(0xFFE8B04A),
        bodyStop = 0.60f,
        edge = Color(0xFFC98A22),
        centerX = 0.35f,
        centerY = 0.35f,
    )
    val GoldGlow = Color(0xFFE8B04A)
    val RoseShadow = Color(0xFF9E3552)
    val LavenderShadow = Color(0xFF5E4A94)
}

/** CSS-style `radial-gradient(circle at x% y%, …)`: the radius reaches the farthest corner. */
fun GradientStops.toBrush(size: Size): Brush {
    val center = Offset(size.width * centerX, size.height * centerY)
    val radius = hypot(
        max(center.x, size.width - center.x),
        max(center.y, size.height - center.y),
    )
    return Brush.radialGradient(
        0f to highlight,
        bodyStop to body,
        1f to edge,
        center = center,
        radius = radius,
    )
}
