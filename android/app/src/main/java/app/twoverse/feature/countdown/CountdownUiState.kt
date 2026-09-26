package app.twoverse.feature.countdown

import app.twoverse.core.common.CountdownTime
import java.time.LocalDate
import java.time.LocalTime

data class CountdownUiState(
    val content: CountdownContent = CountdownContent.Loading,
    val isDatePickerOpen: Boolean = false,
    /** The user's local date; the picker only allows today or later. */
    val today: LocalDate? = null,
)

sealed interface CountdownContent {
    data object Loading : CountdownContent

    /** No date is set (FR-CNT-7), or the last reunion has arrived and passed (FR-CNT-5). */
    data class NoDate(val afterReunion: Boolean) : CountdownContent

    data class Counting(
        val timeLeft: CountdownTime,
        /** Share of the wait already behind the couple, 0..1. */
        val progress: Float,
        /** Reunion date and time in the user's local time zone (FR-CNT-6). */
        val date: LocalDate,
        val time: LocalTime?,
        val place: String?,
    ) : CountdownContent
}
