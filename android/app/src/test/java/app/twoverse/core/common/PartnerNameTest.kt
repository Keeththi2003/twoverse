package app.twoverse.core.common

import app.twoverse.core.model.Pronouns
import app.twoverse.core.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class PartnerNameTest {

    @Test
    fun myNicknameComesFirst() {
        assertEquals("Chellam", partnerDisplayName(nickname = "Chellam", shortName = "Ammu", fullName = "Ammu Perera"))
    }

    @Test
    fun thenTheirShortName() {
        assertEquals("Ammu", partnerDisplayName(nickname = null, shortName = "Ammu", fullName = "Amaya Perera"))
        assertEquals("Ammu", partnerDisplayName(nickname = "  ", shortName = " Ammu ", fullName = "Amaya Perera"))
    }

    @Test
    fun thenTheFirstWordOfTheirFullName() {
        assertEquals("Amaya", partnerDisplayName(nickname = null, shortName = null, fullName = "  Amaya   Perera "))
        assertEquals("Amaya", partnerDisplayName(nickname = "", shortName = "", fullName = "Amaya Perera"))
    }

    @Test
    fun theShortNameDefaultsToTheFirstWordAtMost30Characters() {
        assertEquals("Keeththi", defaultShortName("Keeththi Lan"))
        assertEquals("Dee", defaultShortName("Dee"))
        assertEquals("x".repeat(30), defaultShortName("x".repeat(40) + " Smith"))
    }

    @Test
    fun aProfileBecomesTheNameAndPronounsTextsUse() {
        val partner = UserProfile(id = "p", fullName = "Ammu Perera", shortName = "Ammu", pronouns = Pronouns.She, nickname = "Kanna")
        assertEquals(PartnerName("Kanna", Pronouns.She), partner.toPartnerName())
        assertEquals(PartnerName("Ammu", null), partner.copy(nickname = null, pronouns = null).toPartnerName())
    }
}
