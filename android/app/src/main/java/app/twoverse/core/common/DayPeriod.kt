package app.twoverse.core.common

import java.time.LocalTime

/** Part of the day, for greetings like "Good evening". */
enum class DayPeriod {
    Morning, Afternoon, Evening;

    companion object {
        private val AfternoonStart: LocalTime = LocalTime.NOON
        private val EveningStart: LocalTime = LocalTime.of(17, 0)
        private val MorningStart: LocalTime = LocalTime.of(5, 0)

        fun of(time: LocalTime): DayPeriod = when {
            time < MorningStart -> Evening
            time < AfternoonStart -> Morning
            time < EveningStart -> Afternoon
            else -> Evening
        }
    }
}
