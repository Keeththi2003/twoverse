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
    /** Your profile (FR-PRO-4). */
    val onOpenProfile: () -> Unit = {},
    val onNicknameChange: (String) -> Unit = {},
    val onSaveNickname: () -> Unit = {},
    val onClearNickname: () -> Unit = {},
    /** Opens mail or the dialer for the partner's shared email or phone (FR-PRO-5). */
    val onEmailPartner: (String) -> Unit = {},
    val onCallPartner: (String) -> Unit = {},
    /** Turning sharing on goes through the permission flow (FR-LOC-1). */
    val onOpenLocationSetup: () -> Unit = {},
)
