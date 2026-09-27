package app.twoverse.core.data.fake

import app.twoverse.core.data.ReunionRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.Reunion
import app.twoverse.core.model.ReunionPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import javax.inject.Inject

/** Sample reunion for previews and tests; [failNextWith] makes the next call fail. */
class FakeReunionRepository @Inject constructor() : ReunionRepository {
    private val current = MutableStateFlow<Reunion?>(SampleData.reunion)

    override val reunion: StateFlow<Reunion?> = current

    var failNextWith: DataError? = null

    override suspend fun saveReunion(plan: ReunionPlan): DataResult<Unit> = respond {
        val previous = current.value
        current.value = Reunion(
            meetAt = plan.meetAt,
            hasTime = plan.hasTime,
            place = plan.place,
            note = plan.note,
            dateSetAt = if (previous?.meetAt == plan.meetAt) previous.dateSetAt else Instant.now(),
        )
    }

    override suspend fun clearReunion(): DataResult<Unit> = respond { current.value = null }

    private fun respond(action: () -> Unit): DataResult<Unit> {
        val error = failNextWith
        failNextWith = null
        return if (error != null) DataResult.Failure(error) else DataResult.Success(action())
    }
}
