package app.twoverse.feature.vault

import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class VaultFilterTest {

    private fun memory(id: String, sender: MemorySender, expires: Boolean) = Memory(
        id = id,
        sender = sender,
        imageUrl = null,
        caption = null,
        createdAt = Instant.EPOCH,
        expiresAt = if (expires) Instant.MAX else null,
        allowKeep = false,
        viewedAt = null,
    )

    private val memories = listOf(
        memory("1", MemorySender.Partner, expires = false),
        memory("2", MemorySender.Me, expires = true),
        memory("3", MemorySender.Partner, expires = true),
        memory("4", MemorySender.Me, expires = false),
    )

    private fun ids(filter: VaultFilter) = memories.filteredBy(filter).map { it.id }

    @Test
    fun allKeepsEveryMemoryInOrder() {
        assertEquals(listOf("1", "2", "3", "4"), ids(VaultFilter.All))
    }

    @Test
    fun fromMeKeepsOnlyMemoriesISent() {
        assertEquals(listOf("2", "4"), ids(VaultFilter.FromMe))
    }

    @Test
    fun fromPartnerKeepsOnlyReceivedMemories() {
        assertEquals(listOf("1", "3"), ids(VaultFilter.FromPartner))
    }

    @Test
    fun expiringKeepsOnlyTemporaryMemories() {
        assertEquals(listOf("2", "3"), ids(VaultFilter.Expiring))
    }

    @Test
    fun emptyListStaysEmpty() {
        VaultFilter.entries.forEach { assertEquals(emptyList<Memory>(), emptyList<Memory>().filteredBy(it)) }
    }
}
