package app.twoverse.feature.settings

import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPrecision

/** Everything the settings screen can ask for, grouped to keep the screen signature readable. */
data class SettingsActions(
    val onShareLocationChange: (Boolean) -> Unit = {},
    val onLockOursChange: (Boolean) -> Unit = {},
    val onOpenDialog: (SettingsDialog) -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onLocationPrecisionSelected: (LocationPrecision) -> Unit = {},
    val onDistanceUnitSelected: (DistanceUnit) -> Unit = {},
    val onAppearanceSelected: (AppearanceMode) -> Unit = {},
    val onLogOutConfirmed: () -> Unit = {},
    val onDisconnectConfirmed: () -> Unit = {},
    val onDeleteAccountConfirmed: () -> Unit = {},
    val onShowBirthday: () -> Unit = {},
)
