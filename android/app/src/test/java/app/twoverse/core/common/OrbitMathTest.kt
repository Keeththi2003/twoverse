package app.twoverse.core.common

import app.twoverse.core.model.Meetup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Period

class OrbitMathTest {

    private fun date(text: String): LocalDate = LocalDate.parse(text)

    private fun meetup(start: String, end: String? = null, id: String = start) =
        Meetup(id = id, startDate = date(start), endDate = end?.let(::date), place = null, note = null)

    // Together duration (FR-ORB-4)

    @Test
    fun theStartDateIsDayOne() {
        val duration = togetherDuration(date("2024-02-14"), date("2024-02-14"))

        assertEquals(Period.ZERO, duration?.period)
        assertEquals(1L, duration?.totalDays)
    }

    @Test
    fun durationIsYearsMonthsAndDaysPlusTheDayNumber() {
        val duration = togetherDuration(date("2024-02-14"), date("2026-05-19"))

        assertEquals(Period.of(2, 3, 5), duration?.period)
        // 825 days have passed, so it is day 826.
        assertEquals(826L, duration?.totalDays)
    }

    @Test
    fun leapDaysAreCounted() {
        assertEquals(3L, togetherDuration(date("2024-02-28"), date("2024-03-01"))?.totalDays)
        assertEquals(2L, togetherDuration(date("2025-02-28"), date("2025-03-01"))?.totalDays)
    }

    @Test
    fun monthEndsFollowJavaTime() {
        // From the 31st, a shorter month isn't a whole month yet; the end of the next long month is.
        assertEquals(Period.ofDays(28), togetherDuration(date("2025-01-31"), date("2025-02-28"))?.period)
        assertEquals(Period.ofMonths(2), togetherDuration(date("2025-01-31"), date("2025-03-31"))?.period)
        assertEquals(Period.ofMonths(1), togetherDuration(date("2025-01-28"), date("2025-02-28"))?.period)
        assertEquals(Period.ofDays(29), togetherDuration(date("2024-01-31"), date("2024-02-29"))?.period)
    }

    @Test
    fun aDateThatHasNotBegunYetHasNoDuration() {
        assertNull(togetherDuration(date("2026-10-13"), date("2026-10-12")))
    }

    // Anniversaries (FR-ORB-5)

    @Test
    fun theNextAnniversaryAndDaysUntilIt() {
        val anniversary = nextAnniversary(date("2024-02-14"), date("2026-01-01"))

        assertEquals(2, anniversary.years)
        assertEquals(date("2026-02-14"), anniversary.date)
        assertEquals(44L, anniversary.daysUntil)
        assertFalse(anniversary.isToday)
    }

    @Test
    fun theAnniversaryItselfCountsAsToday() {
        val anniversary = nextAnniversary(date("2024-02-14"), date("2026-02-14"))

        assertEquals(2, anniversary.years)
        assertTrue(anniversary.isToday)
    }

    @Test
    fun theDayAfterLooksToNextYear() {
        val anniversary = nextAnniversary(date("2024-02-14"), date("2026-02-15"))

        assertEquals(3, anniversary.years)
        assertEquals(date("2027-02-14"), anniversary.date)
    }

    @Test
    fun onTheFirstDayTheFirstAnniversaryIsAYearAway() {
        val anniversary = nextAnniversary(date("2026-10-12"), date("2026-10-12"))

        assertEquals(1, anniversary.years)
        assertEquals(date("2027-10-12"), anniversary.date)
        assertEquals(365L, anniversary.daysUntil)
    }

    @Test
    fun aLeapDayAnniversaryFallsOnTheTwentyEighthInOtherYears() {
        val since = date("2024-02-29")

        assertEquals(date("2025-02-28"), nextAnniversary(since, date("2025-01-01")).date)
        assertTrue(nextAnniversary(since, date("2025-02-28")).isToday)
        assertEquals(date("2026-02-28"), nextAnniversary(since, date("2025-03-01")).date)
        assertEquals(date("2028-02-29"), nextAnniversary(since, date("2027-03-01")).date)
        assertEquals(4, nextAnniversary(since, date("2027-03-01")).years)
    }

    // Milestones (FR-ORB-6)

    @Test
    fun dayMilestonesAreTheFirstFourThenEveryThousand() {
        assertEquals(100L, nextDayMilestone(1))
        assertEquals(100L, nextDayMilestone(100))
        assertEquals(365L, nextDayMilestone(101))
        assertEquals(500L, nextDayMilestone(366))
        assertEquals(1000L, nextDayMilestone(501))
        assertEquals(1000L, nextDayMilestone(1000))
        assertEquals(2000L, nextDayMilestone(1001))
        assertEquals(3000L, nextDayMilestone(2001))
    }

    @Test
    fun theMilestoneBeforeEachOne() {
        assertEquals(0L, previousDayMilestone(100))
        assertEquals(100L, previousDayMilestone(365))
        assertEquals(365L, previousDayMilestone(500))
        assertEquals(500L, previousDayMilestone(1000))
        assertEquals(1000L, previousDayMilestone(2000))
        assertEquals(2000L, previousDayMilestone(3000))
    }

    @Test
    fun dayOneHundredIsNinetyNineDaysAfterTheStart() {
        val since = date("2024-01-01")

        val milestone = nextMilestone(since, date("2024-03-01"))

        assertEquals(Milestone.Days(100), milestone.milestone)
        assertEquals(date("2024-04-09"), milestone.date)
        assertEquals(39L, milestone.daysUntil)
        assertTrue(nextMilestone(since, date("2024-04-09")).isToday)
        assertEquals(date("2024-04-09"), dateOfDay(since, 100))
    }

    @Test
    fun anAnniversaryCountsAsAMilestone() {
        val since = date("2024-01-01")

        // Day 365 (30 December) comes before the first anniversary…
        assertEquals(Milestone.Days(365), nextMilestone(since, date("2024-12-01")).milestone)
        // …and after it, the anniversary on 1 January comes before day 500.
        val next = nextMilestone(since, date("2024-12-31"))
        assertEquals(Milestone.Years(1), next.milestone)
        assertEquals(date("2025-01-01"), next.date)
    }

    @Test
    fun milestonesKeepComingEveryThousandDays() {
        val since = date("2020-01-01")
        val day1999 = dateOfDay(since, 1999)

        val next = nextMilestone(since, day1999)

        assertEquals(Milestone.Days(2000), next.milestone)
        assertEquals(1L, next.daysUntil)
    }

    // Meetups (FR-ORB-7)

    private val today = date("2026-10-12")

    @Test
    fun noMeetupsYet() {
        assertEquals(MeetupStats(timesMet = 0, daysTogether = 0, daysSinceLastMet = null), meetupStats(emptyList(), today))
    }

    @Test
    fun aSameDayMeetupIsOneDay() {
        val stats = meetupStats(listOf(meetup("2026-10-02")), today)

        assertEquals(MeetupStats(timesMet = 1, daysTogether = 1, daysSinceLastMet = 10), stats)
    }

    @Test
    fun bothTheFirstAndLastDayCount() {
        assertEquals(5L, meetupStats(listOf(meetup("2026-09-10", "2026-09-14")), today).daysTogether)
    }

    @Test
    fun overlappingMeetupsCountSharedDaysOnce() {
        val stats = meetupStats(listOf(meetup("2026-09-10", "2026-09-14"), meetup("2026-09-12", "2026-09-16")), today)

        assertEquals(2, stats.timesMet)
        assertEquals(7L, stats.daysTogether)
    }

    @Test
    fun twoMeetupsOnTheSameDayAreOneDayTogether() {
        val stats = meetupStats(listOf(meetup("2026-09-10", id = "a"), meetup("2026-09-10", id = "b")), today)

        assertEquals(2, stats.timesMet)
        assertEquals(1L, stats.daysTogether)
    }

    @Test
    fun aMeetupInsideAnotherAddsNothing() {
        val stats = meetupStats(listOf(meetup("2026-09-01", "2026-09-20"), meetup("2026-09-05", "2026-09-06")), today)

        assertEquals(20L, stats.daysTogether)
    }

    @Test
    fun meetupsAcrossMonthEndsAndLeapDays() {
        assertEquals(4L, meetupStats(listOf(meetup("2025-01-30", "2025-02-02")), today).daysTogether)
        assertEquals(3L, meetupStats(listOf(meetup("2024-02-28", "2024-03-01")), today).daysTogether)
        assertEquals(2L, meetupStats(listOf(meetup("2025-02-28", "2025-03-01")), today).daysTogether)
    }

    @Test
    fun separateMeetupsAddUp() {
        val stats = meetupStats(listOf(meetup("2026-01-01", "2026-01-03"), meetup("2026-09-29", "2026-09-30")), today)

        assertEquals(5L, stats.daysTogether)
        assertEquals(12L, stats.daysSinceLastMet)
    }

    @Test
    fun anOngoingVisitCountsUpToTodayAndMeansTogetherNow() {
        val stats = meetupStats(listOf(meetup("2026-10-10", "2026-10-15")), today)

        assertEquals(3L, stats.daysTogether)
        assertEquals(0L, stats.daysSinceLastMet)
    }

    @Test
    fun aMeetupThatHasNotStartedAddsNoDays() {
        val stats = meetupStats(listOf(meetup("2026-10-20")), today)

        assertEquals(1, stats.timesMet)
        assertEquals(0L, stats.daysTogether)
        assertNull(stats.daysSinceLastMet)
    }
}
