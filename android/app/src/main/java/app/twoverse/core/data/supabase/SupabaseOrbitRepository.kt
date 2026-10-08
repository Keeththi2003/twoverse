package app.twoverse.core.data.supabase

import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.OrbitRepository
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.data.local.OfflineCache
import app.twoverse.core.data.local.snapshotFor
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.Meetup
import app.twoverse.core.model.MeetupDraft
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Together since" lives on the couple, so it arrives live with the couple row; it changes only
 * through set_together_since(). Meetups are in the meetups table: RLS limits them to the active
 * couple, and Realtime brings the partner's changes (FR-ORB-12).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SupabaseOrbitRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val coupleRepository: CoupleRepository,
    private val offlineCache: OfflineCache,
    @ApplicationScope appScope: CoroutineScope,
) : OrbitRepository {

    /** Asks the live flow to re-read after this user's own change. */
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val togetherSince: Flow<LocalDate?> = coupleRepository.couple
        .map { it?.togetherSince }
        .distinctUntilChanged()

    override val meetups: Flow<List<Meetup>> = coupleRepository.couple
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { coupleId -> if (coupleId == null) flowOf(emptyList()) else liveMeetups(coupleId) }
        .distinctUntilChanged()
        .shareIn(appScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), replay = 1)

    override suspend fun setTogetherSince(date: LocalDate): DataResult<Unit> = supabaseCall {
        supabase.postgrest.rpc("set_together_since", buildJsonObject { put("p_date", date.toString()) })
        Unit
    }

    override suspend fun saveMeetup(id: String?, draft: MeetupDraft): DataResult<Unit> {
        val coupleId = coupleRepository.couple.first()?.id ?: return DataResult.Failure(DataError.NotPaired)
        val userId = supabase.auth.currentUserOrNull()?.id ?: return DataResult.Failure(DataError.Unknown)
        val values = buildJsonObject {
            put("start_date", draft.startDate.toString())
            put("end_date", draft.endDate?.toString())
            put("place", draft.place?.trim()?.ifEmpty { null })
            put("note", draft.note?.trim()?.ifEmpty { null })
        }
        return supabaseCall {
            if (id == null) {
                val link = draft.fromReunionAt?.let { mapOf("from_reunion_at" to JsonPrimitive(it.toString())) }.orEmpty()
                supabase.from(Table).insert(
                    JsonObject(values + link + mapOf("couple_id" to JsonPrimitive(coupleId), "created_by" to JsonPrimitive(userId))),
                )
            } else {
                supabase.from(Table).update(values) { filter { eq("id", id) } }
            }
            refresh.tryEmit(Unit)
            Unit
        }
    }

    override suspend fun deleteMeetup(id: String): DataResult<Unit> = supabaseCall {
        supabase.from(Table).delete { filter { eq("id", id) } }
        refresh.tryEmit(Unit)
        Unit
    }

    /**
     * Inserts and edits arrive through Realtime filtered to the couple. Deletes can't be filtered,
     * so they arrive through a re-read every minute and after this user's own changes. The saved
     * meetups are shown until the server answers (NFR-REL-1).
     */
    private fun liveMeetups(coupleId: String): Flow<List<Meetup>> = channelFlow {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@channelFlow
        suspend fun publish(meetups: List<Meetup>) {
            send(meetups)
            offlineCache.update(userId) { saved -> if (saved.couple?.id == coupleId) saved.copy(meetups = meetups) else saved }
        }
        offlineCache.snapshotFor(userId)
            ?.takeIf { it.couple?.id == coupleId }
            ?.meetups
            ?.let { send(it) }
        val channel = supabase.channel("meetups:$coupleId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = Table
            filter("couple_id", FilterOperator.EQ, coupleId)
        }
        suspend fun reread() {
            (supabaseCall { fetch(coupleId) } as? DataResult.Success)?.let { publish(it.value) }
        }
        launch { changes.collect { reread() } }
        launch { refresh.collect { reread() } }
        channel.subscribe()
        reread()
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

    private suspend fun fetch(coupleId: String): List<Meetup> =
        supabase.from(Table)
            .select {
                filter { eq("couple_id", coupleId) }
                order("start_date", Order.DESCENDING)
                order("created_at", Order.DESCENDING)
            }
            .decodeList<MeetupDto>()
            .map { it.toModel() }

    private companion object {
        const val Table = "meetups"
        const val StopTimeoutMillis = 5_000L
        const val RefreshMillis = 60_000L
    }
}
