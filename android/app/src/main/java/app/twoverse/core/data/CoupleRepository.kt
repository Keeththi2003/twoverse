package app.twoverse.core.data

import app.twoverse.core.model.Couple
import app.twoverse.core.model.CoupleCode
import kotlinx.coroutines.flow.Flow

interface CoupleRepository {
    /** The active couple, or null when the user isn't paired. */
    val couple: Flow<Couple?>

    suspend fun generateCode(): CoupleCode

    /** Fails with [InvalidCoupleCodeException] for invalid, expired or used codes (FR-PAIR-5). */
    suspend fun joinWithCode(code: String): Result<Couple>

    suspend fun disconnect()
}

class InvalidCoupleCodeException : IllegalArgumentException("Invalid, expired or used couple code")
