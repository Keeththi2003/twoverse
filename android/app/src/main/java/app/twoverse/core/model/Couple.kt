package app.twoverse.core.model

import java.time.Instant
import java.time.LocalDate

data class Couple(
    val id: String,
    val partner: UserProfile,
    val status: CoupleStatus,
    /** When the couple paired in Twoverse; not when the relationship began. */
    val connectedAt: Instant?,
    /** When the relationship began (FR-ORB-1); null until a partner sets it. */
    val togetherSince: LocalDate? = null,
)

enum class CoupleStatus { Pending, Active, Ended }

/** A couple one partner disconnected, still inside its 7-day grace period (SRS 12). */
data class EndedCouple(
    val id: String,
    val endedAt: Instant,
    /** After this, the couple and everything they shared is deleted. */
    val deleteAfter: Instant,
    val reconnectRequest: ReconnectRequest,
)

/** Who has asked to reconnect; both partners must confirm (SRS 12). */
enum class ReconnectRequest { None, ByMe, ByPartner }

enum class ReconnectResult {
    /** Waiting for the partner to confirm. */
    Requested,

    /** Both confirmed; the couple and its data are back. */
    Reconnected,
}
