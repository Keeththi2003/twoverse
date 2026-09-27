package app.twoverse.feature.location

/** Buttons of the location setup flow, grouped to keep the screen signature readable. */
data class LocationSetupActions(
    val onContinue: () -> Unit = {},
    val onNotNow: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onAllowAllTheTime: () -> Unit = {},
    val onWhileUsingOnly: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenBatterySettings: () -> Unit = {},
    val onBatteryGuideDone: () -> Unit = {},
)
