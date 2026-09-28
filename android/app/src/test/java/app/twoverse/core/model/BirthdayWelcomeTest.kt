package app.twoverse.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BirthdayWelcomeTest {

    private val today = LocalDate.of(2026, 9, 28)

    private fun welcome(showOn: LocalDate? = null, seen: Boolean = false) =
        BirthdayWelcome(message = "Happy birthday", fromName = "Her", photoUrl = null, showOn = showOn, seen = seen)

    @Test
    fun withoutADateItShowsOnTheNextOpen() {
        assertTrue(welcome().isDue(today))
    }

    @Test
    fun withADateItWaitsForThatDay() {
        assertFalse(welcome(showOn = today.plusDays(1)).isDue(today))
        assertTrue(welcome(showOn = today).isDue(today))
    }

    @Test
    fun itStillShowsWhenTheAppIsFirstOpenedAfterTheDate() {
        assertTrue(welcome(showOn = today.minusDays(3)).isDue(today))
    }

    @Test
    fun itShowsOnlyOnce() {
        assertFalse(welcome(seen = true).isDue(today))
        assertFalse(welcome(showOn = today, seen = true).isDue(today))
    }
}
