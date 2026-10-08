package app.twoverse.core.data.fake

import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.MyProfile
import app.twoverse.core.model.ProfileEdit
import app.twoverse.core.model.ProfileSettings
import app.twoverse.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Returns the sample user and partner profiles. */
class FakeProfileRepository @Inject constructor() : ProfileRepository {
    private val mine = MutableStateFlow<MyProfile?>(
        MyProfile(
            fullName = SampleData.me.fullName,
            shortName = SampleData.me.shortName.orEmpty(),
            pronouns = SampleData.me.pronouns,
            phone = null,
            shareEmail = false,
            sharePhone = false,
            email = "you@example.com",
            pendingEmail = null,
            canChangeEmail = true,
        ),
    )

    override val myProfile: StateFlow<MyProfile?> = mine

    /** The partner as get_partner_profile() returns them; null when unpaired. */
    var partner: UserProfile? = SampleData.partner

    /** Every nickname saved for the partner (null clears), for tests. */
    val savedNicknames = mutableListOf<String?>()

    fun setMyProfile(profile: MyProfile?) {
        mine.value = profile
    }

    override suspend fun updateMyProfile(edit: ProfileEdit): DataResult<Unit> {
        takeFailure()?.let { return DataResult.Failure(it) }
        mine.value = mine.value?.let { current ->
            current.copy(
                fullName = edit.fullName ?: current.fullName,
                shortName = edit.shortName ?: current.shortName,
                pronouns = edit.pronouns ?: current.pronouns,
                phone = if (edit.clearPhone) null else edit.phone ?: current.phone,
                shareEmail = edit.shareEmail ?: current.shareEmail,
                sharePhone = edit.sharePhone ?: current.sharePhone,
            )
        }
        return DataResult.Success(Unit)
    }

    override suspend fun changeEmail(newEmail: String): DataResult<Unit> {
        takeFailure()?.let { return DataResult.Failure(it) }
        mine.value = mine.value?.copy(pendingEmail = newEmail)
        return DataResult.Success(Unit)
    }

    override suspend fun partnerProfile(): DataResult<UserProfile?> =
        takeFailure()?.let { DataResult.Failure(it) } ?: DataResult.Success(partner)

    override suspend fun setPartnerNickname(nickname: String?): DataResult<Unit> {
        takeFailure()?.let { return DataResult.Failure(it) }
        savedNicknames += nickname
        partner = partner?.copy(nickname = nickname)
        return DataResult.Success(Unit)
    }

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
