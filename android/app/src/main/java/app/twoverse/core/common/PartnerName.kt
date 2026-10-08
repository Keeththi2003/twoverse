package app.twoverse.core.common

import app.twoverse.core.model.Pronouns
import app.twoverse.core.model.UserProfile

/** Short names and nicknames are at most this long (FR-PRO-1). */
const val MaxShortNameLength = 30

/** The partner as the app talks about them: the name to show and their pronouns (FR-PRO-1, FR-PRO-2). */
data class PartnerName(val name: String, val pronouns: Pronouns?)

/** The first word of [fullName], at most [MaxShortNameLength] characters; what a new user is called. */
fun defaultShortName(fullName: String): String =
    fullName.trim().split(Regex("\\s+")).first().take(MaxShortNameLength)

/**
 * The partner's name everywhere outside profile screens: my nickname for them, else their short
 * name, else the first word of their full name (FR-PRO-1).
 */
fun partnerDisplayName(nickname: String?, shortName: String?, fullName: String): String =
    nickname?.trim()?.takeIf { it.isNotEmpty() }
        ?: shortName?.trim()?.takeIf { it.isNotEmpty() }
        ?: defaultShortName(fullName)

fun UserProfile.toPartnerName(): PartnerName =
    PartnerName(name = partnerDisplayName(nickname, shortName, fullName), pronouns = pronouns)
