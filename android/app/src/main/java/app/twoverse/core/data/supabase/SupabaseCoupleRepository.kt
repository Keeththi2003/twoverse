package app.twoverse.core.data.supabase

import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.model.Couple
import app.twoverse.core.model.CoupleCode
import app.twoverse.core.model.CoupleStatus
import app.twoverse.core.model.DataResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseCoupleRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository,
    @ApplicationScope appScope: CoroutineScope,
) : CoupleRepository {

    /**
     * The signed-in user's active couple, refreshed whenever a couples row they can see changes
     * (RLS limits Realtime to their own couples). One subscription is shared by all screens and
     * closed five seconds after the last one stops listening.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override val couple: Flow<Couple?> = supabase.auth.sessionStatus
        .map { (it as? SessionStatus.Authenticated)?.session?.user?.id }
        .distinctUntilChanged()
        .flatMapLatest { userId -> if (userId == null) flowOf(null) else liveCouple(userId) }
        .distinctUntilChanged()
        .shareIn(appScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), replay = 1)

    override suspend fun createCode(): DataResult<CoupleCode> = supabaseCall {
        supabase.postgrest.rpc("create_couple_code").decodeList<CoupleCodeDto>().first().toModel()
    }

    override suspend fun join(code: String): DataResult<Unit> = supabaseCall {
        supabase.postgrest.rpc("join_couple", buildJsonObject { put("p_code", code) })
        Unit
    }

    override suspend fun disconnect(): DataResult<Unit> = supabaseCall {
        supabase.postgrest.rpc("disconnect_couple")
        Unit
    }

    private fun liveCouple(userId: String): Flow<Couple?> = channelFlow {
        val channel = supabase.channel("couples:$userId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "couples" }
        launch { changes.collect { send(loadActiveCouple(userId)) } }
        channel.subscribe()
        send(loadActiveCouple(userId))
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { supabase.realtime.removeChannel(channel) }
        }
    }

    /**
     * Retries until it gets an answer, so a network error is never mistaken for "not paired"
     * (which would send the user to the pairing screen).
     */
    private suspend fun loadActiveCouple(userId: String): Couple? {
        var backoff = InitialRetryMillis
        while (true) {
            when (val result = supabaseCall { fetchActiveCouple(userId) }) {
                is DataResult.Success -> return result.value
                is DataResult.Failure -> {
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MaxRetryMillis)
                }
            }
        }
    }

    private suspend fun fetchActiveCouple(userId: String): Couple? {
        val row = supabase.from("couples")
            .select { filter { eq("status", "active") } }
            .decodeList<CoupleDto>()
            .firstOrNull() ?: return null
        val partnerId = if (row.userA == userId) row.userB else row.userA
        val partner = partnerId?.let { profileRepository.profile(it) } as? DataResult.Success
            ?: error("Partner profile unavailable")
        return Couple(
            id = row.id,
            partner = partner.value,
            status = CoupleStatus.Active,
            connectedAt = row.connectedAt?.let(::parseTimestamp),
        )
    }

    private companion object {
        const val StopTimeoutMillis = 5_000L
        const val InitialRetryMillis = 2_000L
        const val MaxRetryMillis = 30_000L
    }
}
