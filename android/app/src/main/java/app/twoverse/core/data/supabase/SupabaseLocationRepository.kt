package app.twoverse.core.data.supabase

import app.twoverse.core.common.forPrecision
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.data.local.OfflineCache
import app.twoverse.core.data.local.snapshotFor
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DevicePosition
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.LocationSharing
import app.twoverse.core.model.UserLocation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SupabaseLocationRepository @Inject constructor(
    private val supabase: SupabaseClient,
    coupleRepository: CoupleRepository,
    private val offlineCache: OfflineCache,
    @ApplicationScope appScope: CoroutineScope,
) : LocationRepository {

    /** The signed-in user's own row, updated after every write. Null row = nothing saved yet. */
    private val ownRow = MutableStateFlow<OwnRow?>(null)

    private val signedInUserId: Flow<String?> = supabase.auth.sessionStatus
        .filterNot { it is SessionStatus.Initializing }
        .map { (it as? SessionStatus.Authenticated)?.session?.user?.id }
        .distinctUntilChanged()

    /** The saved settings and position come first, so Home works offline (NFR-REL-1). */
    private val own: Flow<OwnState?> = signedInUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf<OwnState?>(null)
            } else {
                flow<OwnState?> {
                    offlineCache.snapshotFor(userId)?.let { saved ->
                        saved.sharing?.let { emit(OwnState(it, saved.myLocation)) }
                    }
                    ownRow.value = OwnRow(userId, retryUntilLoaded { fetchRow(userId) })
                    emitAll(
                        ownRow.filterNotNull()
                            .filter { it.userId == userId }
                            .map { OwnState(it.row?.toSharing() ?: LocationSharing(), it.row?.toModel()) }
                            .onEach { state ->
                                offlineCache.update(userId) { it.copy(sharing = state.sharing, myLocation = state.location) }
                            },
                    )
                }
            }
        }
        .shareIn(appScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), replay = 1)

    override val sharing: Flow<LocationSharing> = own
        .map { it?.sharing ?: LocationSharing() }
        .distinctUntilChanged()

    override val myLocation: Flow<UserLocation?> = own
        .map { it?.location }
        .distinctUntilChanged()

    override val partnerLocation: Flow<UserLocation?> = coupleRepository.couple
        .map { it?.partner?.id }
        .distinctUntilChanged()
        .flatMapLatest { partnerId -> if (partnerId == null) flowOf(null) else livePartner(partnerId) }
        .distinctUntilChanged()
        .shareIn(appScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), replay = 1)

    override suspend fun setSharingEnabled(enabled: Boolean): DataResult<Unit> =
        write(buildJsonObject { put("sharing_enabled", enabled) })

    override suspend fun setPrecision(precision: LocationPrecision): DataResult<Unit> =
        write(buildJsonObject { put("precision", precision.toColumn()) })

    override suspend fun upload(position: DevicePosition): DataResult<Unit> {
        val sharing = currentOwnRow()?.row?.toSharing() ?: return DataResult.Success(Unit)
        if (!sharing.enabled) return DataResult.Success(Unit)
        val rounded = position.forPrecision(sharing.precision)
        return write(
            buildJsonObject {
                put("lat", rounded.latitude)
                put("lng", rounded.longitude)
                put("accuracy_m", rounded.accuracyMeters)
            },
        )
    }

    /** Upserts the user's single row (FR-LOC-7) with only the given columns changed. */
    private suspend fun write(columns: JsonObject): DataResult<Unit> {
        val userId = awaitUserId() ?: return DataResult.Failure(DataError.Unknown)
        return supabaseCall {
            val row = supabase.from(Table)
                .upsert(JsonObject(columns + ("user_id" to JsonPrimitive(userId)))) {
                    onConflict = "user_id"
                    select()
                }
                .decodeSingle<LocationDto>()
            ownRow.value = OwnRow(userId, row)
        }
    }

    private suspend fun currentOwnRow(): OwnRow? {
        val userId = awaitUserId() ?: return null
        ownRow.value?.takeIf { it.userId == userId }?.let { return it }
        return when (val result = supabaseCall { fetchRow(userId) }) {
            is DataResult.Success -> OwnRow(userId, result.value).also { ownRow.value = it }
            is DataResult.Failure -> null
        }
    }

    /** Waits for the saved session to load, so background uploads work after a cold start. */
    private suspend fun awaitUserId(): String? = signedInUserId.first()

    private suspend fun fetchRow(userId: String): LocationDto? =
        supabase.from(Table)
            .select { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<LocationDto>()

    /**
     * The partner's position: pushed by Realtime, and re-read every minute because Realtime
     * can't tell us when the partner stops sharing (their row just becomes invisible to us).
     */
    private fun livePartner(partnerId: String): Flow<UserLocation?> = channelFlow {
        val userId = awaitUserId() ?: return@channelFlow
        suspend fun publish(location: UserLocation?) {
            send(location)
            offlineCache.update(userId) { it.copy(partnerLocation = location) }
        }
        offlineCache.snapshotFor(userId)?.partnerLocation
            ?.takeIf { it.userId == partnerId }
            ?.let { send(it) }
        val channel = supabase.channel("locations:$partnerId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = Table
            filter("user_id", FilterOperator.EQ, partnerId)
        }
        launch { changes.collect { refreshPartner(partnerId)?.let { publish(it.location) } } }
        channel.subscribe()
        publish(retryUntilLoaded { fetchRow(partnerId) }?.toModel())
        launch {
            while (true) {
                delay(PartnerRefreshMillis)
                refreshPartner(partnerId)?.let { publish(it.location) }
            }
        }
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { supabase.realtime.removeChannel(channel) }
        }
    }

    /** Null when the read failed (keep showing the last position rather than guessing). */
    private suspend fun refreshPartner(partnerId: String): PartnerRead? =
        (supabaseCall { fetchRow(partnerId) } as? DataResult.Success)?.let { PartnerRead(it.value?.toModel()) }

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

    private data class OwnRow(val userId: String, val row: LocationDto?)

    private data class OwnState(val sharing: LocationSharing, val location: UserLocation?)

    private data class PartnerRead(val location: UserLocation?)

    private companion object {
        const val Table = "locations"
        const val StopTimeoutMillis = 5_000L
        const val PartnerRefreshMillis = 60_000L
        const val InitialRetryMillis = 2_000L
        const val MaxRetryMillis = 30_000L
    }
}
