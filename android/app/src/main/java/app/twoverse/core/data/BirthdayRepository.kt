package app.twoverse.core.data

import app.twoverse.core.model.BirthdayWelcome
import kotlinx.coroutines.flow.Flow

interface BirthdayRepository {
    /** The birthday welcome for this user, or null when none is configured. */
    val welcome: Flow<BirthdayWelcome?>

    suspend fun markSeen()
}
