package app.twoverse.core.data

import app.twoverse.core.model.DataResult
import app.twoverse.core.model.Reunion
import app.twoverse.core.model.ReunionPlan
import kotlinx.coroutines.flow.Flow

interface ReunionRepository {
    /** The couple's shared next reunion, live for both partners (FR-CNT-2); null when none is set. */
    val reunion: Flow<Reunion?>

    /** Sets or edits the date, time, place and note (FR-CNT-1). */
    suspend fun saveReunion(plan: ReunionPlan): DataResult<Unit>

    suspend fun clearReunion(): DataResult<Unit>
}
