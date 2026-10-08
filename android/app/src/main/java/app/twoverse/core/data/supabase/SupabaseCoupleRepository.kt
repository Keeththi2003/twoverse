package app.twoverse.core.data.supabase

import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.ProfileRepository
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.data.local.OfflineCache
import app.twoverse.core.data.local.snapshotFor
import app.twoverse.core.data.local.withoutCouple
import app.twoverse.core.model.Couple
import app.twoverse.core.model.CoupleCode
import app.twoverse.core.model.CoupleStatus
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.EndedCouple
import app.twoverse.core.model.ReconnectRequest
import app.twoverse.core.model.ReconnectResult
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
import kotlinx.coroutines.flow.MutableSharedFlow
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseCoupleRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository,
    private val offlineCache: OfflineCache,
    private val clock: Clock,
    @ApplicationScope appScope: CoroutineScope,
) : CoupleRepository {

    /** Asks the live state to re-read after this user's own change. */
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * The signed-in user's active couple and any couple still in its disconnect grace period,
     * refreshed whenever a couples row they can see changes (RLS limits Realtime to their own
     * couples). The couple saved on this device is shown until the server answers, so the app
     * opens offline (NFR-REL-1). One subscription is shared by all screens.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val state: Flow<CoupleState?> = supabase.auth.sessionStatus
        .map { (it as? SessionStatus.Authenticated)?.session?.user?.id }
        .distinctUntilChanged()
        .flatMapLatest { userId -> if (userId == null) flowOf(null) else liveState(userId) }
        .distinctUntilChanged()
        .shareIn(appScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), replay = 1)

    override val couple: Flow<Couple?> = state.map { it?.active }.distinctUntilChanged()

    override val endedCouple: Flow<EndedCouple?> = state.map { it?.ended }.distinctUntilChanged()

    override suspend fun createCode(): DataResult<CoupleCode> = supabaseCall {
        supabase.postgrest.rpc("create_couple_code").decodeList<CoupleCodeDto>().first().toModel()
    }

    override suspend fun join(code: String): DataResult<Unit> = supabaseCall {
        supabase.postgrest.rpc("join_couple", buildJsonObject { put("p_code", code) })
        refresh.tryEmit(Unit)
        Unit
    }

    override suspend fun disconnect(): DataResult<Unit> = supabaseCall {
        supabase.postgrest.rpc("disconnect_couple")
        refresh.tryEmit(Unit)
        Unit
    }

    override fun refresh() {
        refresh.tryEmit(Unit)
    }

    override suspend fun reconnect(): DataResult<ReconnectResult> = supabaseCall {
        val status = supabase.postgrest.rpc("reconnect_couple").decodeAs<String>()
        refresh.tryEmit(Unit)
        if (status == ReconnectedStatus) ReconnectResult.Reconnected else ReconnectResult.Requested
    }

    private fun liveState(userId: String): Flow<CoupleState> = channelFlow {
        val channel = supabase.channel("couples:$userId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = Table }
        suspend fun publish(loaded: CoupleState) {
            send(loaded)
            saveOffline(userId, loaded.active)
        }
        offlineCache.snapshotFor(userId)?.couple?.let { send(CoupleState(active = it, ended = null)) }
        launch { changes.collect { publish(loadState(userId)) } }
        launch { refresh.collect { publish(loadState(userId)) } }
        channel.subscribe()
        publish(loadState(userId))
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { supabase.realtime.removeChannel(channel) }
        }
    }

    /** A new or no couple drops the old couple's saved data (partner, reunion, memories). */
    private suspend fun saveOffline(userId: String, active: Couple?) {
        offlineCache.update(userId) { saved ->
            when {
                active == null -> saved.withoutCouple()
                saved.couple?.id != active.id -> saved.withoutCouple().copy(couple = active)
                else -> saved.copy(couple = active)
            }
        }
    }

    /**
     * Retries until it gets an answer, so a network error is never mistaken for "not paired"
     * (which would send the user to the pairing screen).
     */
    private suspend fun loadState(userId: String): CoupleState {
        var backoff = InitialRetryMillis
        while (true) {
            when (val result = supabaseCall { fetchState(userId) }) {
                is DataResult.Success -> return result.value
                is DataResult.Failure -> {
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MaxRetryMillis)
                }
            }
        }
    }

    private suspend fun fetchState(userId: String): CoupleState {
        val rows = supabase.from(Table)
            .select { filter { neq("status", "pending") } }
            .decodeList<CoupleDto>()
        val active = rows.firstOrNull { it.status == "active" }
        if (active != null) return CoupleState(active = activeCouple(active), ended = null)
        val now = clock.instant()
        val ended = rows
            .filter { it.status == "ended" && it.purgeAfter != null && parseTimestamp(it.purgeAfter).isAfter(now) }
            .maxByOrNull { it.endedAt?.let(::parseTimestamp) ?: Instant.MIN }
        return CoupleState(active = null, ended = ended?.toEndedCouple(userId))
    }

    private suspend fun activeCouple(row: CoupleDto): Couple {
        // Only what the partner shares, plus my nickname for them (FR-PRO-5).
        val partner = (profileRepository.partnerProfile() as? DataResult.Success)?.value
            ?: error("Partner profile unavailable")
        return Couple(
            id = row.id,
            partner = partner,
            status = CoupleStatus.Active,
            connectedAt = row.connectedAt?.let(::parseTimestamp),
            togetherSince = row.togetherSince?.let(LocalDate::parse),
        )
    }

    private fun CoupleDto.toEndedCouple(userId: String) = EndedCouple(
        id = id,
        endedAt = endedAt?.let(::parseTimestamp) ?: Instant.EPOCH,
        deleteAfter = parseTimestamp(checkNotNull(purgeAfter)),
        reconnectRequest = when (reconnectRequestedBy) {
            null -> ReconnectRequest.None
            userId -> ReconnectRequest.ByMe
            else -> ReconnectRequest.ByPartner
        },
    )

    private data class CoupleState(val active: Couple?, val ended: EndedCouple?)

    private companion object {
        const val Table = "couples"
        const val ReconnectedStatus = "active"
        const val StopTimeoutMillis = 5_000L
        const val InitialRetryMillis = 2_000L
        const val MaxRetryMillis = 30_000L
    }
}
