package app.twoverse.core.data

import app.twoverse.core.model.Couple
import app.twoverse.core.model.CoupleCode
import app.twoverse.core.model.DataResult
import kotlinx.coroutines.flow.Flow

interface CoupleRepository {
    /** The active couple, or null when unpaired. Updates live when the partner joins (FR-PAIR-6). */
    val couple: Flow<Couple?>

    /** A fresh one-time code valid for 24 hours (FR-PAIR-1). */
    suspend fun createCode(): DataResult<CoupleCode>

    /** Joins the partner's couple (FR-PAIR-3); invalid, expired or used codes fail (FR-PAIR-5). */
    suspend fun join(code: String): DataResult<Unit>

    /** Ends the couple (FR-PAIR-7, BR-9). */
    suspend fun disconnect(): DataResult<Unit>
}
