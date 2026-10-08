package app.twoverse.widget

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.designsystem.theme.DarkTwoverseColors
import app.twoverse.core.designsystem.theme.LightTwoverseColors
import app.twoverse.core.designsystem.theme.TwoverseColors
import app.twoverse.core.designsystem.theme.toColorScheme

/**
 * How the widget chooses light or dark (FR-WGT-5): System follows the phone's dark mode; Light and
 * Dark force that theme, whatever the phone uses.
 */
internal enum class WidgetThemeMode { FollowSystem, Light, Dark }

internal fun widgetThemeMode(appearance: AppearanceMode): WidgetThemeMode = when (appearance) {
    AppearanceMode.System -> WidgetThemeMode.FollowSystem
    AppearanceMode.Light -> WidgetThemeMode.Light
    AppearanceMode.Dark -> WidgetThemeMode.Dark
}

/**
 * The widget's drawables for a theme. The launcher draws them with the phone's configuration, so
 * following the system uses the day/night ones and a forced theme uses fixed light or dark copies.
 */
internal data class WidgetDrawables(
    @param:DrawableRes val background: Int,
    @param:DrawableRes val card: Int,
    @param:DrawableRes val orbitSmall: Int,
    @param:DrawableRes val orbitMedium: Int,
    @param:DrawableRes val orbitLarge: Int,
    @param:DrawableRes val planetYou: Int,
    @param:DrawableRes val planetHer: Int,
    @param:DrawableRes val dotLive: Int,
    @param:DrawableRes val dotMuted: Int,
    @param:DrawableRes val calendar: Int,
    @param:DrawableRes val heart: Int,
)

internal fun widgetDrawables(mode: WidgetThemeMode): WidgetDrawables = when (mode) {
    WidgetThemeMode.FollowSystem -> WidgetDrawables(
        background = R.drawable.widget_background,
        card = R.drawable.widget_card_background,
        orbitSmall = R.drawable.widget_orbit_small,
        orbitMedium = R.drawable.widget_orbit_medium,
        orbitLarge = R.drawable.widget_orbit_large,
        planetYou = R.drawable.widget_planet_you,
        planetHer = R.drawable.widget_planet_her,
        dotLive = R.drawable.widget_dot_live,
        dotMuted = R.drawable.widget_dot_muted,
        calendar = R.drawable.widget_ic_calendar,
        heart = R.drawable.widget_ic_heart,
    )
    WidgetThemeMode.Light -> WidgetDrawables(
        background = R.drawable.widget_background_light,
        card = R.drawable.widget_card_background_light,
        orbitSmall = R.drawable.widget_orbit_small_light,
        orbitMedium = R.drawable.widget_orbit_medium_light,
        orbitLarge = R.drawable.widget_orbit_large_light,
        planetYou = R.drawable.widget_planet_you_light,
        planetHer = R.drawable.widget_planet_her_light,
        dotLive = R.drawable.widget_dot_live_light,
        dotMuted = R.drawable.widget_dot_muted_light,
        calendar = R.drawable.widget_ic_calendar_light,
        heart = R.drawable.widget_ic_heart_light,
    )
    WidgetThemeMode.Dark -> WidgetDrawables(
        background = R.drawable.widget_background_dark,
        card = R.drawable.widget_card_background_dark,
        orbitSmall = R.drawable.widget_orbit_small_dark,
        orbitMedium = R.drawable.widget_orbit_medium_dark,
        orbitLarge = R.drawable.widget_orbit_large_dark,
        planetYou = R.drawable.widget_planet_you_dark,
        planetHer = R.drawable.widget_planet_her_dark,
        dotLive = R.drawable.widget_dot_live_dark,
        dotMuted = R.drawable.widget_dot_muted_dark,
        calendar = R.drawable.widget_ic_calendar_dark,
        heart = R.drawable.widget_ic_heart_dark,
    )
}

/** The fixed app colours for a forced theme; null when following the system. */
internal fun forcedColors(mode: WidgetThemeMode): TwoverseColors? = when (mode) {
    WidgetThemeMode.FollowSystem -> null
    WidgetThemeMode.Light -> LightTwoverseColors
    WidgetThemeMode.Dark -> DarkTwoverseColors
}

/** Twoverse tokens the Material colour scheme has no slot for, and the theme's drawables. */
internal data class WidgetPalette(
    val goldText: ColorProvider,
    val drawables: WidgetDrawables,
    /** Text colour for the live clocks (RemoteViews) in a forced theme; null uses the day/night resource. */
    val clockText: Color?,
)

private val LocalWidgetPalette = staticCompositionLocalOf {
    WidgetPalette(
        goldText = ColorProvider(LightTwoverseColors.goldText),
        drawables = widgetDrawables(WidgetThemeMode.FollowSystem),
        clockText = null,
    )
}

/** The app's colour tokens (night navy in dark, blush in light) in the theme [mode] picks (FR-WGT-5). */
@Composable
internal fun WidgetTheme(mode: WidgetThemeMode, content: @Composable () -> Unit) {
    val fixed = forcedColors(mode)
    val colors = if (fixed == null) {
        ColorProviders(light = LightTwoverseColors.toColorScheme(), dark = DarkTwoverseColors.toColorScheme())
    } else {
        ColorProviders(fixed.toColorScheme())
    }
    val palette = WidgetPalette(
        goldText = if (fixed == null) {
            ColorProvider(day = LightTwoverseColors.goldText, night = DarkTwoverseColors.goldText)
        } else {
            ColorProvider(fixed.goldText)
        },
        drawables = widgetDrawables(mode),
        clockText = fixed?.onSurface,
    )
    GlanceTheme(colors = colors) {
        CompositionLocalProvider(LocalWidgetPalette provides palette, content = content)
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
        base.background(ImageProvider(widgetPalette.drawables.background))
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
    val CardNumberCompact: TextUnit = 20.sp
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
