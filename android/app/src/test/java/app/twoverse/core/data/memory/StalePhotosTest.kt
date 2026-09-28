package app.twoverse.core.data.memory

import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class StalePhotosTest {

    private val now = Instant.parse("2026-09-26T12:00:00Z")

    private fun memory(id: String, expiresAt: Instant? = null) = Memory(
        id = id,
        sender = MemorySender.Partner,
        imageUrl = null,
        caption = null,
        createdAt = now - Duration.ofDays(1),
        expiresAt = expiresAt,
        allowKeep = false,
        viewedAt = null,
    )

    @Test
    fun photosOfVisibleMemoriesStay() {
        val memories = listOf(memory("a"), memory("b", expiresAt = now + Duration.ofHours(1)))

        assertEquals(emptySet<String>(), stalePhotos(setOf("a", "b"), memories, now))
    }

    @Test
    fun photosOfDeletedOrHiddenMemoriesAreRemoved() {
        assertEquals(setOf("gone"), stalePhotos(setOf("a", "gone"), listOf(memory("a")), now))
    }

    @Test
    fun photosOfExpiredMemoriesAreRemovedEvenBeforeTheServerDropsThem() {
        val memories = listOf(memory("a"), memory("expired", expiresAt = now))

        assertEquals(setOf("expired"), stalePhotos(setOf("a", "expired"), memories, now))
    }

    @Test
    fun withoutMemoriesEveryPhotoIsRemoved() {
        assertEquals(setOf("a", "b"), stalePhotos(setOf("a", "b"), emptyList(), now))
    }
}
