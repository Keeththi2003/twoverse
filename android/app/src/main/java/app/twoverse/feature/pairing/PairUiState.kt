package app.twoverse.feature.pairing

data class PairUiState(
    val coupleCode: String? = null,
    val codeExpiresInHours: Long = 0,
    val isWaitingForPartner: Boolean = true,
    val partnerCode: String = "",
    val isConnecting: Boolean = false,
    val isInvalidCode: Boolean = false,
    val isConnected: Boolean = false,
) {
    val canConnect: Boolean get() = partnerCode.isNotBlank() && !isConnecting
}
