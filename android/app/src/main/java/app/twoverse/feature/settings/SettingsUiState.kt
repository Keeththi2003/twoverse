package app.twoverse.feature.settings

import app.twoverse.core.common.toPartnerName
import app.twoverse.core.model.DataError
import app.twoverse.core.model.UserProfile
import app.twoverse.core.model.UserSettings
import java.time.LocalDate

data class SettingsUiState(
    /** Null while loading. */
    val settings: UserSettings? = null,
    val isConnected: Boolean = false,
    /** When the couple connected, in the user's local time zone (FR-SET-1). */
    val connectedSince: LocalDate? = null,
    /** The partner's details while connected (FR-PRO-5); email and phone only when shared. */
    val partner: UserProfile? = null,
    val openDialog: SettingsDialog? = null,
    /** The nickname being typed in the nickname dialog. */
    val nicknameDraft: String = "",
    /** Log out, disconnect or account deletion failed on the server. */
    val error: DataError? = null,
    /** Set when the user left their account or couple, so the app can navigate away. */
    val exit: SettingsExit? = null,
) {
    /** My nickname for the partner, else their short name (FR-PRO-1); null when unpaired. */
    val partnerDisplayName: String? get() = partner?.toPartnerName()?.name
}

enum class SettingsDialog { LocationPrecision, DistanceUnit, Appearance, LogOut, Disconnect, DeleteAccount, Nickname }

enum class SettingsExit { SignedOut, Disconnected }
