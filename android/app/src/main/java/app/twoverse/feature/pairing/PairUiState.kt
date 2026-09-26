package app.twoverse.feature.pairing

data class PairUiState(
    val coupleCode: String? = null,
    val codeExpiresInHours: Long = 0,
    val isWaitingForPartner: Boolean = true,
    val partnerCode: String = "",
    val isConnecting: Boolean = false,
    val isInvalidCode: Boolean = false,
    val isConnected: Boolean = false,
    /** After connecting, whether an unseen birthday welcome is waiting (FR-BDY-3). */
    val hasBirthdayWelcome: Boolean = false,
) {
    val canConnect: Boolean get() = partnerCode.isNotBlank() && !isConnecting
}
