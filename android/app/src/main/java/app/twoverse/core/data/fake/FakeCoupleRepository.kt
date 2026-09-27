package app.twoverse.core.data.fake

import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.Couple
import app.twoverse.core.model.CoupleCode
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * In-memory pairing for previews and tests. Starts unpaired; any well-formed `XXXX-XXXX` code
 * pairs with the sample partner unless a test sets [failNextWith].
 */
class FakeCoupleRepository @Inject constructor() : CoupleRepository {
    private val activeCouple = MutableStateFlow<Couple?>(null)

    override val couple: StateFlow<Couple?> = activeCouple

    /** The next call fails with this error (then it resets). */
    var failNextWith: DataError? = null

    override suspend fun createCode(): DataResult<CoupleCode> = respond { SampleData.coupleCode }

    override suspend fun join(code: String): DataResult<Unit> {
        if (!CodeFormat.matches(code.trim().uppercase())) return DataResult.Failure(DataError.InvalidCoupleCode)
        return respond { activeCouple.value = SampleData.couple }
    }

    override suspend fun disconnect(): DataResult<Unit> = respond { activeCouple.value = null }

    /** What Realtime does when the partner joins this user's code (FR-PAIR-6). */
    fun simulatePartnerJoined() {
        activeCouple.value = SampleData.couple
    }

    private fun <T> respond(action: () -> T): DataResult<T> {
        val error = failNextWith
        failNextWith = null
        return if (error != null) DataResult.Failure(error) else DataResult.Success(action())
    }

    private companion object {
        val CodeFormat = Regex("[A-Z0-9]{4}-[A-Z0-9]{4}")
    }
}
