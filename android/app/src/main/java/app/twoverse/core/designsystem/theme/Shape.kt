package app.twoverse.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class TwoverseShapes(
    val button: Shape = RoundedCornerShape(28.dp),
    val buttonSmall: Shape = RoundedCornerShape(26.dp),
    val card: Shape = RoundedCornerShape(24.dp),
    val cardLarge: Shape = RoundedCornerShape(28.dp),
    val cardSmall: Shape = RoundedCornerShape(18.dp),
    val group: Shape = RoundedCornerShape(20.dp),
    val input: Shape = RoundedCornerShape(16.dp),
    val chip: Shape = RoundedCornerShape(18.dp),
    val segment: Shape = RoundedCornerShape(14.dp),
    val iconTile: Shape = RoundedCornerShape(16.dp),
    val vaultTile: Shape = RoundedCornerShape(16.dp),
    val circle: Shape = CircleShape,
)

internal val TwoverseMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
