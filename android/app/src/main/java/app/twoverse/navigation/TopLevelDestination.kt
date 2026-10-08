package app.twoverse.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.twoverse.R

/** The five bottom-bar tabs (DESIGN.md §7), in this order. */
enum class TopLevelDestination(
    val route: Any,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Home(HomeRoute, R.string.nav_universe, R.drawable.ic_orbit),
    Compass(CompassRoute, R.string.nav_your_star, R.drawable.ic_compass),
    Vault(VaultRoute, R.string.nav_ours, R.drawable.ic_lock),
    Orbit(OrbitRoute, R.string.nav_our_orbit, R.drawable.ic_our_orbit),
    Settings(SettingsRoute, R.string.nav_us, R.drawable.ic_person),
}
