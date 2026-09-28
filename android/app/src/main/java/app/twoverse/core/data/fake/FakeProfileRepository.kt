package app.twoverse.core.data.fake

import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.ProfileSettings
import app.twoverse.core.model.UserProfile
import javax.inject.Inject

/** Returns the sample user and partner profiles. */
class FakeProfileRepository @Inject constructor() : ProfileRepository {
    override suspend fun profile(userId: String): DataResult<UserProfile> =
        listOf(SampleData.me, SampleData.partner).firstOrNull { it.id == userId }
            ?.let { DataResult.Success(it) }
            ?: DataResult.Failure(DataError.Unknown)

    /** Every time zone saved, for tests. */
    val savedTimeZones = mutableListOf<String>()

    override suspend fun updateTimeZone(zoneId: String): DataResult<Unit> {
        savedTimeZones += zoneId
        return DataResult.Success(Unit)
    }

    /** The settings saved on the server. */
    var serverSettings = ProfileSettings(lockOurs = true, distanceUnit = DistanceUnit.Kilometres, appearance = AppearanceMode.System)
    private var nextFailure: DataError? = null

    /** Makes the next settings call fail with [error], to test error handling. */
    fun failNextWith(error: DataError) {
        nextFailure = error
    }

    override suspend fun settings(): DataResult<ProfileSettings> =
        takeFailure()?.let { DataResult.Failure(it) } ?: DataResult.Success(serverSettings)

    override suspend fun setLockOurs(enabled: Boolean): DataResult<Unit> = saveSettings { it.copy(lockOurs = enabled) }

    override suspend fun setDistanceUnit(unit: DistanceUnit): DataResult<Unit> = saveSettings { it.copy(distanceUnit = unit) }

    override suspend fun setAppearance(appearance: AppearanceMode): DataResult<Unit> =
        saveSettings { it.copy(appearance = appearance) }

    private fun saveSettings(change: (ProfileSettings) -> ProfileSettings): DataResult<Unit> {
        takeFailure()?.let { return DataResult.Failure(it) }
        serverSettings = change(serverSettings)
        return DataResult.Success(Unit)
    }

    private fun takeFailure(): DataError? = nextFailure.also { nextFailure = null }
}
