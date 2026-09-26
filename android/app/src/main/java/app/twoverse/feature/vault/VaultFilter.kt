package app.twoverse.feature.vault

import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender

/** Vault filter chips (FR-VLT-2). */
enum class VaultFilter { All, FromMe, FromPartner, Expiring }

/** Memories matching [filter], in their original (newest-first) order. */
internal fun List<Memory>.filteredBy(filter: VaultFilter): List<Memory> = when (filter) {
    VaultFilter.All -> this
    VaultFilter.FromMe -> filter { it.sender == MemorySender.Me }
    VaultFilter.FromPartner -> filter { it.sender == MemorySender.Partner }
    VaultFilter.Expiring -> filter { it.expiresAt != null }
}
