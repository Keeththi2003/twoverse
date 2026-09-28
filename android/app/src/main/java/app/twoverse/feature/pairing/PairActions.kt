package app.twoverse.feature.pairing

/** Everything the pairing screen can ask for, grouped to keep the screen signature readable. */
data class PairActions(
    /** Null when there is nothing to go back to. */
    val onBack: (() -> Unit)? = null,
    val onShareCode: (String) -> Unit = {},
    val onCopyCode: (String) -> Unit = {},
    val onRetryCode: () -> Unit = {},
    val onPartnerCodeChange: (String) -> Unit = {},
    val onConnect: () -> Unit = {},
    /** Opens the reconnect screen during the disconnect grace period (SRS 12). */
    val onReconnect: () -> Unit = {},
    val onLogOut: () -> Unit = {},
    val onLogOutConfirmed: () -> Unit = {},
    val onLogOutDismissed: () -> Unit = {},
)
