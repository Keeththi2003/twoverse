package app.twoverse.core.model

import java.time.Instant

data class Couple(
    val id: String,
    val partner: UserProfile,
    val status: CoupleStatus,
    val connectedAt: Instant?,
)

enum class CoupleStatus { Pending, Active, Ended }
