package app.twoverse.core.data.fake

import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.InvalidCoupleCodeException
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.Couple
import app.twoverse.core.model.CoupleCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Starts unpaired; any well-formed `XXXX-XXXX` code pairs with the sample partner. */
@Singleton
class FakeCoupleRepository @Inject constructor() : CoupleRepository {
    private val activeCouple = MutableStateFlow<Couple?>(null)

    override val couple: StateFlow<Couple?> = activeCouple

    override suspend fun generateCode(): CoupleCode = SampleData.coupleCode

    override suspend fun joinWithCode(code: String): Result<Couple> {
        if (!CodeFormat.matches(code.trim().uppercase())) {
            return Result.failure(InvalidCoupleCodeException())
        }
        activeCouple.value = SampleData.couple
        return Result.success(SampleData.couple)
    }

    override suspend fun disconnect() {
        activeCouple.value = null
    }

    private companion object {
        val CodeFormat = Regex("[A-Z0-9]{4}-[A-Z0-9]{4}")
    }
}
