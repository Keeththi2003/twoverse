package app.twoverse.feature.vault

import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender
import java.time.Duration
import java.time.Instant

internal val VaultNow: Instant = Instant.parse("2026-09-26T12:00:00Z")

internal fun testMemory(
    id: String,
    sender: MemorySender = MemorySender.Partner,
    sentAgo: Duration = Duration.ofHours(1),
    expiresIn: Duration? = null,
    allowKeep: Boolean = false,
    viewed: Boolean = false,
) = Memory(
    id = id,
    sender = sender,
    imageUrl = null,
    caption = "Caption $id",
    createdAt = VaultNow - sentAgo,
    expiresAt = expiresIn?.let { VaultNow + it },
    allowKeep = allowKeep,
    viewedAt = if (viewed) VaultNow - Duration.ofMinutes(1) else null,
)
