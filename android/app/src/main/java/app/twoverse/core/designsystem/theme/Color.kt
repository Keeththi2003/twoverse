package app.twoverse.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class TwoverseColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val lavender: Color,
    val gold: Color,
    val goldText: Color,
    val chip: Color,
    val error: Color,
    val starSoft: Color,
    val vaultTileTints: List<Color>,
)

private val Plum = Color(0xFF2B1633)

val CardShadowColor = Plum.copy(alpha = 0.07f)

val LightTwoverseColors = TwoverseColors(
    isDark = false,
    background = Color(0xFFF6ECF1),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFBF4F7),
    onSurface = Plum,
    onSurfaceVariant = Color(0xFF6B5572),
    outline = Color(0xFFEAD9E2),
    primary = Color(0xFFC4466A),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFE0607E),
    lavender = Color(0xFF9D86D0),
    gold = Color(0xFFE8B04A),
    goldText = Color(0xFF8A5A0B),
    chip = Color(0xFFF3E1EA),
    error = Color(0xFFB3261E),
    starSoft = Color(0xFFD7A9BF),
    vaultTileTints = listOf(
        Color(0xFFF3E1EA),
        Color(0xFFECE5F7),
        Color(0xFFFBEFD9),
        Color(0xFFFFFFFF),
    ),
)

val DarkTwoverseColors = TwoverseColors(
    isDark = true,
    background = Color(0xFF120E24),
    surface = Color(0xFF1C1733),
    surfaceVariant = Color(0xFF231D3E),
    onSurface = Color(0xFFF6ECF1),
    onSurfaceVariant = Color(0xFFB8AECF),
    outline = Color(0xFF2E2650),
    primary = Color(0xFFF07C98),
    onPrimary = Color(0xFF2A0F1C),
    accent = Color(0xFFF07C98),
    lavender = Color(0xFFA48FDA),
    gold = Color(0xFFF2C46D),
    goldText = Color(0xFFF2C46D),
    chip = Color(0xFF2A2247),
    error = Color(0xFFFF9494),
    starSoft = Color(0xFFE8E2F7),
    vaultTileTints = listOf(
        Color(0xFF2A2247),
        Color(0xFF2B2350),
        Color(0xFF33263A),
        Color(0xFF1C1733),
    ),
)

internal fun TwoverseColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        secondary = lavender,
        tertiary = gold,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainer = surface,
        outline = outline,
        outlineVariant = outline,
        secondaryContainer = chip,
        onSecondaryContainer = onSurface,
        error = error,
    )
}
