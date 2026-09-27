package app.twoverse.core.data.supabase

import app.twoverse.core.data.PushRepository
import app.twoverse.core.data.PushTokenSource
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.PartnerPush
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabasePushRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val tokenSource: PushTokenSource,
) : PushRepository {

    override suspend fun registerThisDevice(token: String?): DataResult<Unit> {
        val deviceToken = token ?: tokenSource.token() ?: return DataResult.Failure(DataError.Unknown)
        return supabaseCall {
            supabase.postgrest.rpc("register_device_token", buildJsonObject { put("p_token", deviceToken) })
            Unit
        }
    }

    override suspend fun unregisterThisDevice(): DataResult<Unit> {
        val token = tokenSource.token() ?: return DataResult.Success(Unit)
        return supabaseCall {
            supabase.from("device_tokens").delete { filter { eq("fcm_token", token) } }
            Unit
        }
    }

    override suspend fun sendToPartner(push: PartnerPush): DataResult<Unit> = supabaseCall {
        supabase.functions.invoke(SendPushFunction, buildJsonObject { put("type", push.type()) })
        Unit
    }

    private fun PartnerPush.type(): String = when (this) {
        PartnerPush.WakeUp -> "wake_up"
        PartnerPush.PartnerJoined -> "partner_joined"
        PartnerPush.NewMemory -> "new_memory"
    }

    private companion object {
        const val SendPushFunction = "send-push"
    }
}
