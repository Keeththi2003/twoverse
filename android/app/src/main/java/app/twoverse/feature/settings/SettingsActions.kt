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
    /** Writes a Shooting Star for the partner (FR-STAR-1). */
    val onSendShootingStar: () -> Unit = {},
    /** Sent and received Shooting Stars (FR-STAR-9, FR-STAR-13). */
    val onOpenShootingStars: () -> Unit = {},
    /** "When did your story begin?" (FR-SET-7, FR-ORB-2). */
    val onSetTogetherSince: () -> Unit = {},
    /** Turning sharing on goes through the permission flow (FR-LOC-1). */
    val onOpenLocationSetup: () -> Unit = {},
)
