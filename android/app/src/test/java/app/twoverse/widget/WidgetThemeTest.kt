package app.twoverse.widget

import app.twoverse.R
import app.twoverse.core.designsystem.theme.DarkTwoverseColors
import app.twoverse.core.designsystem.theme.LightTwoverseColors
import app.twoverse.core.model.AppearanceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetThemeTest {

    @Test
    fun theWidgetFollowsTheAppearanceSetting() {
        assertEquals(WidgetThemeMode.FollowSystem, widgetThemeMode(AppearanceMode.System))
        assertEquals(WidgetThemeMode.Light, widgetThemeMode(AppearanceMode.Light))
        assertEquals(WidgetThemeMode.Dark, widgetThemeMode(AppearanceMode.Dark))
    }

    @Test
    fun followingTheSystemUsesDayNightResources() {
        assertNull(forcedColors(WidgetThemeMode.FollowSystem))
        val drawables = widgetDrawables(WidgetThemeMode.FollowSystem)
        assertEquals(R.drawable.widget_background, drawables.background)
        assertEquals(R.drawable.widget_card_background, drawables.card)
        assertEquals(R.drawable.widget_orbit_large, drawables.orbitLarge)
    }

    @Test
    fun lightForcesTheBlushThemeWhateverThePhoneUses() {
        assertEquals(LightTwoverseColors, forcedColors(WidgetThemeMode.Light))
        val drawables = widgetDrawables(WidgetThemeMode.Light)
        assertEquals(R.drawable.widget_background_light, drawables.background)
        assertEquals(R.drawable.widget_card_background_light, drawables.card)
        assertEquals(R.drawable.widget_orbit_small_light, drawables.orbitSmall)
        assertEquals(R.drawable.widget_orbit_medium_light, drawables.orbitMedium)
        assertEquals(R.drawable.widget_orbit_large_light, drawables.orbitLarge)
        assertEquals(R.drawable.widget_ic_heart_light, drawables.heart)
        assertEquals(R.drawable.widget_ic_calendar_light, drawables.calendar)
    }

    @Test
    fun darkForcesTheNightNavyTheme() {
        assertEquals(DarkTwoverseColors, forcedColors(WidgetThemeMode.Dark))
        val drawables = widgetDrawables(WidgetThemeMode.Dark)
        assertEquals(R.drawable.widget_background_dark, drawables.background)
        assertEquals(R.drawable.widget_card_background_dark, drawables.card)
        assertEquals(R.drawable.widget_orbit_small_dark, drawables.orbitSmall)
        assertEquals(R.drawable.widget_planet_you_dark, drawables.planetYou)
        assertEquals(R.drawable.widget_dot_live_dark, drawables.dotLive)
        assertEquals(R.drawable.widget_dot_muted_dark, drawables.dotMuted)
    }
}
