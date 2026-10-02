package app.twoverse.feature.pairing

import app.twoverse.core.model.DataError
import java.time.LocalDate

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
    /** After connecting, whether a Shooting Star is waiting to be shown (FR-STAR-12). */
    val hasWaitingStar: Boolean = false,
    /** After connecting, ask "When did your story begin?" unless the partner already set it (FR-ORB-2). */
    val askTogetherSince: Boolean = false,
    /** A disconnected couple that can still be restored (SRS 12); null otherwise. */
    val reconnect: PairReconnect? = null,
    val isLogOutDialogOpen: Boolean = false,
    val logOutError: DataError? = null,
    val isSignedOut: Boolean = false,
) {
    val canConnect: Boolean get() = partnerCode.isNotBlank() && !isConnecting
}

data class PairReconnect(
    /** The last day the shared data is kept, in the user's time zone. */
    val deleteOn: LocalDate,
    /** The partner already asked to reconnect. */
    val partnerAsked: Boolean,
)
