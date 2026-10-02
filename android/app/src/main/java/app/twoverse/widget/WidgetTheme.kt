package app.twoverse.widget

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.twoverse.R
import app.twoverse.core.designsystem.theme.DarkTwoverseColors
import app.twoverse.core.designsystem.theme.LightTwoverseColors
import app.twoverse.core.designsystem.theme.TwoverseColors
import app.twoverse.core.designsystem.theme.toColorScheme

/** Twoverse tokens the Material colour scheme has no slot for. */
internal data class WidgetPalette(val goldText: ColorProvider)

private val LocalWidgetPalette = staticCompositionLocalOf { WidgetPalette(goldText = ColorProvider(LightTwoverseColors.goldText)) }

/** The Twoverse palette, switching with the system theme like the rest of the home screen (FR-WGT-5). */
@Composable
internal fun WidgetTheme(content: @Composable () -> Unit) {
    GlanceTheme(colors = ColorProviders(light = LightTwoverseColors.toColorScheme(), dark = DarkTwoverseColors.toColorScheme())) {
        CompositionLocalProvider(
            LocalWidgetPalette provides WidgetPalette(
                goldText = ColorProvider(day = LightTwoverseColors.goldText, night = DarkTwoverseColors.goldText),
            ),
            content = content,
        )
    }
}

/** One fixed theme, for Glance previews, which don't switch between day and night. */
@Composable
internal fun WidgetPreviewTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors: TwoverseColors = if (dark) DarkTwoverseColors else LightTwoverseColors
    GlanceTheme(colors = ColorProviders(colors.toColorScheme())) {
        CompositionLocalProvider(LocalWidgetPalette provides WidgetPalette(goldText = ColorProvider(colors.goldText)), content = content)
    }
}

internal val widgetPalette: WidgetPalette
    @Composable get() = LocalWidgetPalette.current

/**
 * The widget's own background: the theme background with the system widget radius on Android 12+,
 * a rounded drawable with the same colours before that.
 */
@Composable
internal fun GlanceModifier.widgetBackground(): GlanceModifier {
    val base = appWidgetBackground()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        base.background(GlanceTheme.colors.background).cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        base.background(ImageProvider(R.drawable.widget_background))
    }
}

/**
 * Glance can't use the app's Fraunces and Manrope fonts, so the widget keeps their hierarchy with
 * the system serif for numbers and sans for labels (DESIGN.md §3).
 */
internal object WidgetText {
    val DistanceSmall: TextUnit = 28.sp
    val DistanceMedium: TextUnit = 34.sp
    val DistanceLarge: TextUnit = 40.sp
    val Days: TextUnit = 24.sp
    val Status: TextUnit = 15.sp
    val Body: TextUnit = 13.sp
    val Small: TextUnit = 12.sp

    @Composable
    fun serif(size: TextUnit, color: ColorProvider = GlanceTheme.colors.onSurface) =
        TextStyle(color = color, fontSize = size, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)

    @Composable
    fun sans(size: TextUnit, color: ColorProvider = GlanceTheme.colors.onSurface, weight: FontWeight = FontWeight.Medium) =
        TextStyle(color = color, fontSize = size, fontWeight = weight, fontFamily = FontFamily.SansSerif)

    @Composable
    fun muted(size: TextUnit = Body) = sans(size, GlanceTheme.colors.onSurfaceVariant)
}
