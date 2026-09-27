package app.twoverse.core.data.supabase

import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.ReunionRepository
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.Reunion
import app.twoverse.core.model.ReunionPlan
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SupabaseReunionRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val coupleRepository: CoupleRepository,
    @ApplicationScope appScope: CoroutineScope,
) : ReunionRepository {

    /** Asks the live flow to re-read after this user's own change. */
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val reunion: Flow<Reunion?> = coupleRepository.couple
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { coupleId -> if (coupleId == null) flowOf(null) else liveReunion(coupleId) }
        .distinctUntilChanged()
        .shareIn(appScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), replay = 1)

    override suspend fun saveReunion(plan: ReunionPlan): DataResult<Unit> {
        val coupleId = coupleRepository.couple.first()?.id ?: return DataResult.Failure(DataError.NotPaired)
        val userId = supabase.auth.currentUserOrNull()?.id ?: return DataResult.Failure(DataError.Unknown)
        return supabaseCall {
            supabase.from(Table).upsert(
                buildJsonObject {
                    put("couple_id", coupleId)
                    put("meet_at", plan.meetAt.toString())
                    put("has_time", plan.hasTime)
                    plan.place?.let { put("place", it) } ?: put("place", JsonNull)
                    plan.note?.let { put("note", it) } ?: put("note", JsonNull)
                    put("updated_by", userId)
                },
            ) { onConflict = "couple_id" }
            refresh.tryEmit(Unit)
            Unit
        }
    }

    override suspend fun clearReunion(): DataResult<Unit> {
        val coupleId = coupleRepository.couple.first()?.id ?: return DataResult.Failure(DataError.NotPaired)
        return supabaseCall {
            supabase.from(Table).delete { filter { eq("couple_id", coupleId) } }
            refresh.tryEmit(Unit)
            Unit
        }
    }

    /**
     * Realtime brings the partner's edits (FR-CNT-2). Deletes can't be filtered by couple, so a
     * cleared date also arrives through a re-read every minute and after this user's own changes.
     */
    private fun liveReunion(coupleId: String): Flow<Reunion?> = channelFlow {
        val channel = supabase.channel("reunions:$coupleId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = Table
            filter("couple_id", FilterOperator.EQ, coupleId)
        }
        suspend fun reread() {
            (supabaseCall { fetch(coupleId) } as? DataResult.Success)?.let { send(it.value) }
        }
        launch { changes.collect { reread() } }
        launch { refresh.collect { reread() } }
        channel.subscribe()
        send(retryUntilLoaded { fetch(coupleId) })
        launch {
            while (true) {
                delay(RefreshMillis)
                reread()
            }
        }
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { supabase.realtime.removeChannel(channel) }
        }
    }

    private suspend fun fetch(coupleId: String): Reunion? =
        supabase.from(Table)
            .select { filter { eq("couple_id", coupleId) } }
            .decodeSingleOrNull<ReunionDto>()
            ?.toModel()

    private suspend fun <T> retryUntilLoaded(load: suspend () -> T): T {
        var backoff = InitialRetryMillis
        while (true) {
            when (val result = supabaseCall { load() }) {
                is DataResult.Success -> return result.value
                is DataResult.Failure -> {
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MaxRetryMillis)
                }
            }
        }
    }

    private companion object {
        const val Table = "reunions"
        const val StopTimeoutMillis = 5_000L
        const val RefreshMillis = 60_000L
        const val InitialRetryMillis = 2_000L
        const val MaxRetryMillis = 30_000L
    }
}
