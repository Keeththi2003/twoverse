package app.twoverse.core.designsystem.text

import app.twoverse.R
import app.twoverse.core.model.Pronouns
import org.junit.Assert.assertEquals
import org.junit.Test

class PronounStringsTest {

    private val strings = PronounStrings(
        she = R.string.memory_hide_message_she,
        he = R.string.memory_hide_message_he,
        they = R.string.memory_hide_message_they,
    )

    @Test
    fun eachPronounGetsItsOwnSentence() {
        assertEquals(R.string.memory_hide_message_she, strings.resFor(Pronouns.She))
        assertEquals(R.string.memory_hide_message_he, strings.resFor(Pronouns.He))
        assertEquals(R.string.memory_hide_message_they, strings.resFor(Pronouns.They))
    }

    @Test
    fun unknownPronounsUseTheyThem() {
        assertEquals(R.string.memory_hide_message_they, strings.resFor(null))
    }

    @Test
    fun pronounLabels() {
        assertEquals(R.string.pronouns_she, Pronouns.She.labelRes())
        assertEquals(R.string.pronouns_he, Pronouns.He.labelRes())
        assertEquals(R.string.pronouns_they, Pronouns.They.labelRes())
    }
}
