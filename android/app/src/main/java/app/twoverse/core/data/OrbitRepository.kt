package app.twoverse.core.data

import app.twoverse.core.model.DataResult
import app.twoverse.core.model.Meetup
import app.twoverse.core.model.MeetupDraft
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** When the relationship began and the times the couple met (FR-ORB). */
interface OrbitRepository {
    /** When the relationship began; null until a partner sets it. Live for both partners. */
    val togetherSince: Flow<LocalDate?>

    /** The couple's meetups, newest first, live for both partners (FR-ORB-12); empty when unpaired. */
    val meetups: Flow<List<Meetup>>

    /** Sets or changes the date; it can't be in the future (FR-ORB-1). */
    suspend fun setTogetherSince(date: LocalDate): DataResult<Unit>

    /** Adds a meetup ([id] null) or edits one (FR-ORB-3). */
    suspend fun saveMeetup(id: String?, draft: MeetupDraft): DataResult<Unit>

    suspend fun deleteMeetup(id: String): DataResult<Unit>
}
