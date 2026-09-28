package app.twoverse.core.data.supabase

import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.ProfileSettings
import app.twoverse.core.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseProfileRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : ProfileRepository {

    /** RLS only returns the user's own profile and their partner's. */
    override suspend fun profile(userId: String): DataResult<UserProfile> = supabaseCall {
        supabase.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingle<ProfileDto>()
            .toModel()
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
}
