package app.twoverse.core.data.supabase

import app.twoverse.core.common.expiresAt
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.MemoryRepository
import app.twoverse.core.data.di.ApplicationScope
import app.twoverse.core.data.local.OfflineCache
import app.twoverse.core.data.local.snapshotFor
import app.twoverse.core.data.memory.MemoryPhotoCache
import app.twoverse.core.data.memory.MemoryPhotoCompressor
import app.twoverse.core.data.memory.stalePhotos
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemoryDraft
import app.twoverse.core.model.MemoryExpiry
import app.twoverse.core.model.MemoryUpload
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import io.ktor.http.ContentType
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memories in the private "memories" bucket, one folder per couple ({couple_id}/…). RLS decides
 * what each partner sees: unexpired memories of their active couple, minus the ones they hid.
 * Photos are downloaded once through short-lived signed URLs into [MemoryPhotoCache].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SupabaseMemoryRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val coupleRepository: CoupleRepository,
    private val photoCache: MemoryPhotoCache,
    private val compressor: MemoryPhotoCompressor,
    private val offlineCache: OfflineCache,
    private val clock: Clock,
    @ApplicationScope appScope: CoroutineScope,
) : MemoryRepository {

    /** Asks the live list to re-read after this user's own change. */
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Photos being downloaded, so a list update never starts the same download twice. */
    private val downloading = ConcurrentHashMap.newKeySet<String>()
    private val downloadPermits = Semaphore(MaxParallelDownloads)

    private val bucket get() = supabase.storage.from(Bucket)

    override val memories: Flow<List<Memory>> = coupleRepository.couple
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { coupleId -> if (coupleId == null) noMemories() else liveMemories(coupleId) }
        .distinctUntilChanged()
        .shareIn(appScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), replay = 1)

    override fun memory(id: String): Flow<Memory?> = memories.map { list -> list.find { it.id == id } }.distinctUntilChanged()

    override fun send(draft: MemoryDraft): Flow<MemoryUpload> = channelFlow {
        send(MemoryUpload.Uploading(0f))
        val coupleId = coupleRepository.couple.first()?.id
        val userId = supabase.auth.currentUserOrNull()?.id
        if (coupleId == null || userId == null) {
            send(MemoryUpload.Failed(DataError.NotPaired))
            return@channelFlow
        }
        val photo = when (val result = supabaseCall { compressor.compress(draft.photoUri) }) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> {
                send(MemoryUpload.Failed(result.error))
                return@channelFlow
            }
        }
        val path = "$coupleId/${UUID.randomUUID()}.jpg"
        val uploaded = supabaseCall {
            bucket.uploadAsFlow(path, photo) { contentType = ContentType.Image.JPEG }.collect { status ->
                if (status is UploadStatus.Progress && status.contentLength > 0) {
                    send(MemoryUpload.Uploading(status.totalBytesSend.toFloat() / status.contentLength))
                }
            }
        }
        if (uploaded is DataResult.Failure) {
            send(MemoryUpload.Failed(uploaded.error))
            return@channelFlow
        }
        when (val saved = supabaseCall { insert(coupleId, userId, path, draft) }) {
            is DataResult.Success -> {
                photoCache.save(saved.value.id, photo)
                refresh.tryEmit(Unit)
                send(MemoryUpload.Sent(saved.value.id))
            }
            is DataResult.Failure -> {
                supabaseCall { bucket.delete(path) }
                send(MemoryUpload.Failed(saved.error))
            }
        }
    }

    override suspend fun keepForever(id: String): DataResult<Unit> = memoryRpc("keep_memory_forever", id)

    override suspend fun delete(id: String): DataResult<Unit> {
        val result = supabaseCall {
            supabase.from(Table)
                .delete {
                    select()
                    filter { eq("id", id) }
                }
                .decodeList<MemoryDto>()
                .isNotEmpty()
        }
        return when (result) {
            is DataResult.Failure -> result
            is DataResult.Success -> if (result.value) removedLocally(id) else DataResult.Failure(DataError.MemoryUnavailable)
        }
    }

    override suspend fun hide(id: String): DataResult<Unit> =
        when (val result = memoryRpc("hide_memory", id)) {
            is DataResult.Success -> removedLocally(id)
            is DataResult.Failure -> result
        }

    override suspend fun markViewed(id: String): DataResult<Unit> = memoryRpc("mark_memory_viewed", id)

    private suspend fun memoryRpc(function: String, id: String): DataResult<Unit> = supabaseCall {
        supabase.postgrest.rpc(function, buildJsonObject { put("p_memory_id", id) })
        refresh.tryEmit(Unit)
        Unit
    }

    private suspend fun removedLocally(id: String): DataResult<Unit> {
        photoCache.remove(setOf(id))
        refresh.tryEmit(Unit)
        return DataResult.Success(Unit)
    }

    private suspend fun insert(coupleId: String, userId: String, path: String, draft: MemoryDraft): MemoryDto {
        val temporary = draft.expiry != MemoryExpiry.Never
        return supabase.from(Table)
            .insert(
                buildJsonObject {
                    put("couple_id", coupleId)
                    put("sender_id", userId)
                    put("storage_path", path)
                    draft.caption?.let { put("caption", it) } ?: put("caption", JsonNull)
                    draft.expiry.expiresAt(clock.instant())?.let { put("expires_at", it.toString()) }
                    put("allow_keep", temporary && draft.allowKeep)
                },
            ) { select() }
            .decodeSingle<MemoryDto>()
    }

    /**
     * No couple: signed out, unpaired or disconnected, so no photo stays on the device. While the
     * session is still being restored nothing is known yet, so the cache is kept.
     */
    private fun noMemories(): Flow<List<Memory>> = flow {
        if (supabase.auth.sessionStatus.value !is SessionStatus.Initializing) photoCache.clear()
        emit(emptyList())
    }

    /**
     * Realtime brings new, viewed and kept memories (FR-VLT-1). Deletes, hides and expiry can't
     * be delivered that way, so the list is also re-read every minute and after this user's own
     * changes. Every re-read removes photos of memories that are gone (FR-DEL-4). The saved
     * list is shown until the server answers (NFR-REL-1).
     */
    private fun liveMemories(coupleId: String): Flow<List<Memory>> = channelFlow {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@channelFlow
        val rows = MutableStateFlow<List<MemoryDto>?>(null)
        val saved = offlineCache.snapshotFor(userId)?.takeIf { it.couple?.id == coupleId }?.memories
        val channel = supabase.channel("memories:$coupleId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = Table
            filter("couple_id", FilterOperator.EQ, coupleId)
        }
        suspend fun reread() {
            (supabaseCall { fetch() } as? DataResult.Success)?.let { rows.value = it.value }
        }
        launch { changes.collect { reread() } }
        launch { refresh.collect { reread() } }
        launch {
            rows.filterNotNull().collect { list ->
                val memories = list.map { it.toModel(userId, imageUrl = null) }
                offlineCache.update(userId) { snapshot ->
                    if (snapshot.couple?.id == coupleId) snapshot.copy(memories = memories) else snapshot
                }
                photoCache.remove(stalePhotos(photoCache.cachedIds.first(), memories, clock.instant()))
                list.forEach { downloadIfMissing(it) }
            }
        }
        launch {
            combine(rows, photoCache.cachedIds) { list, cached ->
                val memories = list?.map { it.toModel(userId, imageUrl = null) } ?: saved ?: return@combine null
                memories.map { it.copy(imageUrl = if (it.id in cached) photoCache.uriFor(it.id) else null) }
            }.filterNotNull().collect { send(it) }
        }
        channel.subscribe()
        rows.value = retryUntilLoaded { fetch() }
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

    /** Fetches a photo once; a failed download is tried again on the next re-read (NFR-PRF-3). */
    private fun CoroutineScope.downloadIfMissing(row: MemoryDto) = launch {
        if (row.id in photoCache.cachedIds.first() || !downloading.add(row.id)) return@launch
        try {
            downloadPermits.withPermit {
                supabaseCall {
                    val url = bucket.createSignedUrl(row.storagePath, SignedUrlLifetime)
                    photoCache.download(row.id, url)
                }
            }
        } finally {
            downloading.remove(row.id)
        }
    }

    private suspend fun fetch(): List<MemoryDto> =
        supabase.from(Table)
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<MemoryDto>()

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
        const val Table = "memories"
        const val Bucket = "memories"
        val SignedUrlLifetime = 60.seconds
        const val MaxParallelDownloads = 3
        const val StopTimeoutMillis = 5_000L
        const val RefreshMillis = 60_000L
        const val InitialRetryMillis = 2_000L
        const val MaxRetryMillis = 30_000L
    }
}
