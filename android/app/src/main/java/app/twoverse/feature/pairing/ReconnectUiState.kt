package app.twoverse.feature.pairing

import app.twoverse.core.model.DataError
import app.twoverse.core.model.ReconnectRequest
import java.time.LocalDate

/** Restoring a disconnected couple during the 7-day grace period (SRS 12). */
sealed interface ReconnectUiState {
    data object Loading : ReconnectUiState

    /** Nothing to reconnect: the grace period ended or the couple was deleted. */
    data object Unavailable : ReconnectUiState

    /** Both confirmed; the app returns to Our Universe. */
    data object Reconnected : ReconnectUiState

    data class Available(
        /** The last day the shared data is kept, in the user's time zone. */
        val deleteOn: LocalDate,
        val request: ReconnectRequest,
        val isReconnecting: Boolean = false,
        val error: DataError? = null,
    ) : ReconnectUiState {
        /** The user can ask, or confirm the partner's request; not while waiting for the partner. */
        val canReconnect: Boolean get() = request != ReconnectRequest.ByMe && !isReconnecting
    }
}
