package app.twoverse.feature.settings

import app.twoverse.core.model.UserSettings
import java.time.LocalDate

data class SettingsUiState(
    /** Null while loading. */
    val settings: UserSettings? = null,
    val isConnected: Boolean = false,
    /** When the couple connected, in the user's local time zone (FR-SET-1). */
    val connectedSince: LocalDate? = null,
    val openDialog: SettingsDialog? = null,
    /** Set when the user left their account or couple, so the app can navigate away. */
    val exit: SettingsExit? = null,
)

enum class SettingsDialog { LocationPrecision, DistanceUnit, Appearance, LogOut, Disconnect, DeleteAccount }

enum class SettingsExit { SignedOut, Disconnected }
