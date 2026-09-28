package app.twoverse.core.model

import java.time.Instant

data class Memory(
    val id: String,
    val sender: MemorySender,
    /** The photo in app-internal storage (FR-VLT-7), or null until it has been downloaded. */
    val imageUrl: String?,
    val caption: String?,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val allowKeep: Boolean,
    val viewedAt: Instant?,
)

enum class MemorySender { Me, Partner }

/** Received and not opened yet (FR-VLT-3). */
val Memory.isNew: Boolean get() = sender == MemorySender.Partner && viewedAt == null

/** The recipient may keep a temporary memory when the sender allowed it (FR-MEM-5, FR-DEL-2). */
val Memory.canKeepForever: Boolean get() = sender == MemorySender.Partner && allowKeep && expiresAt != null
