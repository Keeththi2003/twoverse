package app.twoverse.core.data.fake

import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.UserProfile
import javax.inject.Inject

/** Returns the sample user and partner profiles. */
class FakeProfileRepository @Inject constructor() : ProfileRepository {
    override suspend fun profile(userId: String): DataResult<UserProfile> =
        listOf(SampleData.me, SampleData.partner).firstOrNull { it.id == userId }
            ?.let { DataResult.Success(it) }
            ?: DataResult.Failure(DataError.Unknown)
}
