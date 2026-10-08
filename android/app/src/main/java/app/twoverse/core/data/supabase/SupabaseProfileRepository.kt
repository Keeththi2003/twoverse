package app.twoverse.core.data.supabase

import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.MyProfile
import app.twoverse.core.model.ProfileEdit
import app.twoverse.core.model.ProfileSettings
import app.twoverse.core.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SupabaseProfileRepository @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationScope appScope: CoroutineScope,
) : ProfileRepository {

    /** Asks [myProfile] to re-read after this user's own change. */
    private val refreshMine = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * RLS only returns the user's own profile and their partner's; the phone and sharing columns
     * aren't selectable, so only the public ones are asked for (FR-PRO-6).
     */
    override suspend fun profile(userId: String): DataResult<UserProfile> = supabaseCall {
        supabase.from("profiles")
            .select(Columns.list(ProfileDto.Columns)) { filter { eq("id", userId) } }
            .decodeSingle<ProfileDto>()
            .toModel()
    }

    override val myProfile: Flow<MyProfile?> = supabase.auth.sessionStatus
        .map { (it as? SessionStatus.Authenticated)?.session?.user?.id }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) flowOf(null) else refreshMine.onStart { emit(Unit) }.map { loadMyProfile() }
        }
        .shareIn(appScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), replay = 1)

    override suspend fun updateMyProfile(edit: ProfileEdit): DataResult<Unit> {
        val result = updateOwn {
            edit.fullName?.let { put("full_name", it.trim()) }
            edit.shortName?.let { put("short_name", it.trim()) }
            edit.pronouns?.let { put("pronouns", it.toColumn()) }
            when {
                edit.clearPhone -> put("phone", JsonNull)
                edit.phone != null -> put("phone", edit.phone)
            }
            edit.shareEmail?.let { put("share_email", it) }
            edit.sharePhone?.let { put("share_phone", it) }
        }
        if (result is DataResult.Success) refreshMine.tryEmit(Unit)
        return result
    }

    override suspend fun changeEmail(newEmail: String): DataResult<Unit> = supabaseCall {
        supabase.auth.updateUser { email = newEmail.trim() }
        refreshMine.tryEmit(Unit)
        Unit
    }

    override suspend fun partnerProfile(): DataResult<UserProfile?> = supabaseCall {
        supabase.postgrest.rpc("get_partner_profile").decodeList<PartnerProfileDto>().firstOrNull()?.toModel()
    }

    override suspend fun setPartnerNickname(nickname: String?): DataResult<Unit> = supabaseCall {
        supabase.postgrest.rpc("set_partner_nickname", buildJsonObject { put("p_nickname", nickname?.trim()) })
        Unit
    }

    /** Retries until it gets an answer, so the About you step never shows by mistake offline. */
    private suspend fun loadMyProfile(): MyProfile {
        var backoff = InitialRetryMillis
        while (true) {
            when (val result = supabaseCall { fetchMyProfile() }) {
                is DataResult.Success -> return result.value
                is DataResult.Failure -> {
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MaxRetryMillis)
                }
            }
        }
    }

    private suspend fun fetchMyProfile(): MyProfile {
        val row = supabase.postgrest.rpc("get_my_profile").decodeList<MyProfileDto>().first()
        val user = supabase.auth.currentUserOrNull()
        return row.toModel(
            email = user?.email,
            pendingEmail = user?.newEmail?.takeIf { it.isNotBlank() && it != user.email },
            canChangeEmail = user?.identities.orEmpty().any { it.provider == EmailProvider },
        )
    }

    override suspend fun updateTimeZone(zoneId: String): DataResult<Unit> = updateOwn { put("time_zone", zoneId) }

    override suspend fun settings(): DataResult<ProfileSettings> {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return DataResult.Failure(DataError.Unknown)
        return supabaseCall {
            supabase.from("profiles")
                .select(Columns.list("lock_ours", "distance_unit", "appearance")) { filter { eq("id", userId) } }
                .decodeSingle<ProfileSettingsDto>()
                .toModel()
        }
    }

    override suspend fun setLockOurs(enabled: Boolean): DataResult<Unit> = updateOwn { put("lock_ours", enabled) }

    override suspend fun setDistanceUnit(unit: DistanceUnit): DataResult<Unit> =
        updateOwn { put("distance_unit", unit.toColumn()) }

    override suspend fun setAppearance(appearance: AppearanceMode): DataResult<Unit> =
        updateOwn { put("appearance", appearance.toColumn()) }

    private suspend fun updateOwn(values: JsonObjectBuilder.() -> Unit): DataResult<Unit> {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return DataResult.Failure(DataError.Unknown)
        return supabaseCall {
            supabase.from("profiles").update(buildJsonObject(values)) {
                filter { eq("id", userId) }
            }
            Unit
        }
    }

    private companion object {
        const val EmailProvider = "email"
        const val StopTimeoutMillis = 5_000L
        const val InitialRetryMillis = 2_000L
        const val MaxRetryMillis = 30_000L
    }
}
