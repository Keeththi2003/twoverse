package app.twoverse.feature.pairing

import app.twoverse.core.model.DataError

data class PairUiState(
    val coupleCode: String? = null,
    val codeExpiresInHours: Long = 0,
    /** Creating this user's code failed; the card offers a retry. */
    val codeError: DataError? = null,
    val isWaitingForPartner: Boolean = true,
    val partnerCode: String = "",
    val isConnecting: Boolean = false,
    /** Joining the partner's code failed (FR-PAIR-5). */
    val joinError: DataError? = null,
    /** Paired, either by joining a code or by the partner joining this user's code (FR-PAIR-6). */
    val isConnected: Boolean = false,
    /** After connecting, whether an unseen birthday welcome is waiting (FR-BDY-3). */
    val hasBirthdayWelcome: Boolean = false,
) {
    val canConnect: Boolean get() = partnerCode.isNotBlank() && !isConnecting
}
