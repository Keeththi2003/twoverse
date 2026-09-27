package app.twoverse.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ReunionPhaseTest {

    private val colombo = ZoneId.of("Asia/Colombo")

    /** Saturday 10 October 2026, 10:00 in Colombo. */
    private val meetAt = Instant.parse("2026-10-10T04:30:00Z")

    @Test
    fun beforeTheMeetingItIsUpcoming() {
        assertEquals(ReunionPhase.Upcoming, reunionPhase(meetAt, meetAt.minusSeconds(1), colombo))
    }

    @Test
    fun fromTheMeetingUntilLocalMidnightIsTheCelebration() {
        assertEquals(ReunionPhase.Today, reunionPhase(meetAt, meetAt, colombo))
        assertEquals(ReunionPhase.Today, reunionPhase(meetAt, Instant.parse("2026-10-10T18:29:59Z"), colombo))
    }

    @Test
    fun theNextLocalDayAsksForTheNextTime() {
        assertEquals(ReunionPhase.Past, reunionPhase(meetAt, Instant.parse("2026-10-10T18:30:00Z"), colombo))
    }

    @Test
    fun theDayIsTheUsersLocalDay() {
        val later = Instant.parse("2026-10-10T20:00:00Z")

        assertEquals(ReunionPhase.Past, reunionPhase(meetAt, later, colombo))
        assertEquals(ReunionPhase.Today, reunionPhase(meetAt, later, ZoneId.of("America/New_York")))
    }
}
