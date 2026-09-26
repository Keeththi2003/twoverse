package app.twoverse.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.twoverse.R

/** The four bottom-bar tabs (DESIGN.md §7). */
enum class TopLevelDestination(
    val route: Any,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Home(HomeRoute, R.string.nav_universe, R.drawable.ic_orbit),
    Compass(CompassRoute, R.string.nav_your_star, R.drawable.ic_compass),
    Vault(VaultRoute, R.string.nav_ours, R.drawable.ic_lock),
    Settings(SettingsRoute, R.string.nav_you_and_her, R.drawable.ic_person),
}
