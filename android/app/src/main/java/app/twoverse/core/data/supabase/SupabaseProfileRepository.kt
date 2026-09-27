package app.twoverse.core.data.supabase

import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
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
}
