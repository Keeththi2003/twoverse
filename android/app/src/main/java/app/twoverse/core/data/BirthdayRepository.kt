package app.twoverse.core.data

import app.twoverse.core.model.BirthdayMessage
import app.twoverse.core.model.BirthdayMessageDraft
import app.twoverse.core.model.BirthdayWelcome
import app.twoverse.core.model.DataResult
import kotlinx.coroutines.flow.Flow

/** Birthday welcomes, stored in the backend (FR-BDY-2). */
interface BirthdayRepository {
    /** The welcome the partner wrote for this user, or null when there is none (or it can't be loaded). */
    val welcome: Flow<BirthdayWelcome?>

    suspend fun markSeen(): DataResult<Unit>

    /** The welcome this user wrote for their partner, or null when there is none yet. */
    suspend fun myMessage(): DataResult<BirthdayMessage?>

    /** Saves the welcome for the partner, who then sees it once more (FR-BDY-1, FR-BDY-3). */
    suspend fun saveMyMessage(draft: BirthdayMessageDraft): DataResult<Unit>
}
