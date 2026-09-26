package app.twoverse.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalTwoverseColors = staticCompositionLocalOf { LightTwoverseColors }
private val LocalTwoverseShapes = staticCompositionLocalOf { TwoverseShapes() }
private val LocalTwoverseSpacing = staticCompositionLocalOf { TwoverseSpacing() }
private val LocalTwoverseTextStyles = staticCompositionLocalOf { TwoverseTextStyles() }

@Composable
fun TwoverseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkTwoverseColors else LightTwoverseColors
    CompositionLocalProvider(
        LocalTwoverseColors provides colors,
        LocalTwoverseShapes provides TwoverseShapes(),
        LocalTwoverseSpacing provides TwoverseSpacing(),
        LocalTwoverseTextStyles provides TwoverseTextStyles(),
    ) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = TwoverseTypography,
            shapes = TwoverseMaterialShapes,
            content = content,
        )
    }
}

/** Brand tokens that Material 3 has no slot for. Standard text styles come from `MaterialTheme.typography`. */
object TwoverseTheme {
    val colors: TwoverseColors
        @Composable @ReadOnlyComposable get() = LocalTwoverseColors.current

    val shapes: TwoverseShapes
        @Composable @ReadOnlyComposable get() = LocalTwoverseShapes.current

    val spacing: TwoverseSpacing
        @Composable @ReadOnlyComposable get() = LocalTwoverseSpacing.current

    val textStyles: TwoverseTextStyles
        @Composable @ReadOnlyComposable get() = LocalTwoverseTextStyles.current
}
