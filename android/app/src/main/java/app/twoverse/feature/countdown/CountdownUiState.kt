package app.twoverse.feature.countdown

import app.twoverse.core.common.CountdownTime
import java.time.LocalDate
import java.time.LocalTime

data class CountdownUiState(
    val content: CountdownContent = CountdownContent.Loading,
)

sealed interface CountdownContent {
    data object Loading : CountdownContent

    /** No date is set (FR-CNT-7). */
    data object NoDate : CountdownContent

    /** The reunion day is over: "When's the next time?" (FR-CNT-5). */
    data object AfterReunion : CountdownContent

    /** The countdown reached zero and it is the reunion day (FR-CNT-5). */
    data class Celebrating(val plan: ReunionDetails) : CountdownContent

    data class Counting(
        val timeLeft: CountdownTime,
        /** Share of the wait already behind the couple, 0..1. */
        val progress: Float,
        val plan: ReunionDetails,
    ) : CountdownContent
}

/** The plan in the user's local time zone (FR-CNT-6). */
data class ReunionDetails(
    val date: LocalDate,
    val time: LocalTime?,
    val place: String?,
    val note: String?,
)
