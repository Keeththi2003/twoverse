package app.twoverse.feature.location

import app.twoverse.core.model.DataError

data class LocationSetupUiState(
    val step: LocationSetupStep = LocationSetupStep.Checking,
    /** A system permission dialog the screen should show next; cleared once shown. */
    val permissionRequest: PermissionRequest? = null,
    val error: DataError? = null,
    /** The flow is over (sharing on, or the user chose not now). */
    val isFinished: Boolean = false,
)

enum class LocationSetupStep {
    /** Reading the current permissions. */
    Checking,

    /** Why location is needed, before the system dialog (FR-LOC-1). */
    Explain,
    Denied,

    /** Android won't show the dialog again; only system settings can change it. */
    DeniedForever,

    /** Why "Allow all the time" helps (FR-LOC-4). */
    Background,
    Enabling,

    /** Samsung battery settings, shown once after sharing is first turned on. */
    BatteryGuide,
}

enum class PermissionRequest { Foreground, Background }
