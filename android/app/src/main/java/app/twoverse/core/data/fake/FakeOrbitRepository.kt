package app.twoverse.core.data.fake

import app.twoverse.core.data.OrbitRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.Meetup
import app.twoverse.core.model.MeetupDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject

/** In-memory Our Orbit data for previews and tests; starts with no date and no meetups. */
class FakeOrbitRepository @Inject constructor() : OrbitRepository {
    private val since = MutableStateFlow<LocalDate?>(null)
    private val saved = MutableStateFlow<List<Meetup>>(emptyList())
    private var nextFailure: DataError? = null
    private var nextId = 1

    override val togetherSince: StateFlow<LocalDate?> = since.asStateFlow()

    override val meetups: StateFlow<List<Meetup>> = saved.asStateFlow()

    fun setTogetherSince(date: LocalDate?) {
        since.value = date
    }

    fun setMeetups(meetups: List<Meetup>) {
        saved.value = meetups.sortedByDescending { it.startDate }
    }

    /** Makes the next call fail with [error], to test error handling. */
    fun failNextWith(error: DataError) {
        nextFailure = error
    }

    override suspend fun setTogetherSince(date: LocalDate): DataResult<Unit> = respond { since.value = date }

    override suspend fun saveMeetup(id: String?, draft: MeetupDraft): DataResult<Unit> = respond {
        val meetup = Meetup(
            id = id ?: "meetup-${nextId++}",
            startDate = draft.startDate,
            endDate = draft.endDate,
            place = draft.place?.trim()?.ifEmpty { null },
            note = draft.note?.trim()?.ifEmpty { null },
            fromReunionAt = draft.fromReunionAt ?: saved.value.firstOrNull { it.id == id }?.fromReunionAt,
        )
        setMeetups(saved.value.filterNot { it.id == meetup.id } + meetup)
    }

    override suspend fun deleteMeetup(id: String): DataResult<Unit> = respond {
        saved.value = saved.value.filterNot { it.id == id }
    }

    private fun respond(action: () -> Unit): DataResult<Unit> {
        val error = nextFailure
        nextFailure = null
        return if (error != null) DataResult.Failure(error) else DataResult.Success(action())
    }
}
