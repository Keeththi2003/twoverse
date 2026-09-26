package app.twoverse.core.data

import app.twoverse.core.model.Reunion
import kotlinx.coroutines.flow.Flow

interface ReunionRepository {
    /** The next reunion, or null when no date is set. */
    val reunion: Flow<Reunion?>

    suspend fun setReunion(reunion: Reunion)

    suspend fun clearReunion()
}
