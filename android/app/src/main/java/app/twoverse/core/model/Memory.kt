package app.twoverse.core.model

import java.time.Instant

data class Memory(
    val id: String,
    val sender: MemorySender,
    val imageUrl: String?,
    val caption: String?,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val allowKeep: Boolean,
    val viewedAt: Instant?,
)

enum class MemorySender { Me, Partner }
