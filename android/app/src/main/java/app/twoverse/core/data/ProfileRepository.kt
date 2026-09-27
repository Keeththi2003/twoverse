package app.twoverse.core.data

import app.twoverse.core.model.DataResult
import app.twoverse.core.model.UserProfile

interface ProfileRepository {
    /** The profile of the signed-in user or their partner; others are not readable. */
    suspend fun profile(userId: String): DataResult<UserProfile>
}
