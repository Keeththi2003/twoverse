package app.twoverse.core.data.supabase

import app.twoverse.core.common.StarPhotoLongEdge
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.ShootingStarRepository
import app.twoverse.core.data.memory.MemoryPhotoCompressor
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.ShootingStar
import app.twoverse.core.model.ShootingStarDraft
import app.twoverse.core.model.StarField
import app.twoverse.core.model.StarPhotoChange
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/**
 * Shooting Stars in the shooting_stars table (FR-STAR-1). RLS shows the recipient a star only once
 * it is visible and lets only the sender change it while unseen (FR-STAR-15). Photos go to the
 * couple's stars folder in the private "memories" bucket and are shown through short-lived
 * signed URLs (FR-STAR-16).
 */
@Singleton
class SupabaseShootingStarRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val coupleRepository: CoupleRepository,
    private val compressor: MemoryPhotoCompressor,
) : ShootingStarRepository {

    /** Re-reads after this user marks a star seen. */
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val bucket get() = supabase.storage.from(Bucket)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val received: Flow<List<ShootingStar>> = coupleRepository.couple
        .distinctUntilChanged { old, new -> old?.id == new?.id }
        .flatMapLatest { couple ->
            if (couple == null) flowOf(emptyList()) else refresh.onStart { emit(Unit) }.map { fetchReceived() }
        }
        .distinctUntilChanged()

    override suspend fun sent(): DataResult<List<ShootingStar>> {
        val userId = currentUserId() ?: return DataResult.Failure(DataError.Unknown)
        return supabaseCall {
            supabase.from(Table)
                .select {
                    filter { eq("sender_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ShootingStarDto>()
                .map { it.toModel() }
        }
    }

    override suspend fun star(id: String): DataResult<ShootingStar> {
        val row = when (val result = supabaseCall { fetchRow(id) }) {
            is DataResult.Failure -> return result
            is DataResult.Success -> result.value ?: return DataResult.Failure(DataError.StarUnavailable)
        }
        return supabaseCall { row.toModel(photoUrl = row.photoPath?.let { signedUrl(it) }) }
    }

    override suspend fun markSeen(id: String): DataResult<Unit> = supabaseCall {
        supabase.postgrest.rpc("mark_shooting_star_seen", buildJsonObject { put("p_star_id", id) })
        refresh.tryEmit(Unit)
        Unit
    }

    /**
     * Uploads a new photo first, then saves the row. Fields the layout doesn't use are cleared,
     * and the database deletes a photo that is replaced or removed. An edit that finds no row
     * means the partner saw the star in the meantime (or it was deleted).
     */
    override suspend fun save(id: String?, draft: ShootingStarDraft): DataResult<Unit> {
        val couple = coupleRepository.couple.first() ?: return DataResult.Failure(DataError.NotPaired)
        val userId = currentUserId() ?: return DataResult.Failure(DataError.Unknown)
        val content = draft.content.normalized()
        val usesPhoto = content.layout.shows(StarField.Photo)
        val newPath = (draft.photo as? StarPhotoChange.Replace)?.takeIf { usesPhoto }?.let { replace ->
            when (val uploaded = supabaseCall { upload(couple.id, replace.photoUri) }) {
                is DataResult.Success -> uploaded.value
                is DataResult.Failure -> return uploaded
            }
        }
        val values = buildJsonObject {
            put("layout", content.layout.toColumn())
            put("eyebrow", content.eyebrow)
            put("title", content.title)
            put("message", content.message)
            put("signature", content.signature)
            put("photo_fit", content.photoFit.toColumn())
            put("show_at", draft.showAt?.toString())
            when {
                newPath != null -> put("photo_path", newPath)
                !usesPhoto || draft.photo == StarPhotoChange.Remove -> put("photo_path", JsonNull)
                else -> Unit
            }
        }
        val saved = supabaseCall {
            if (id == null) {
                supabase.from(Table).insert(
                    JsonObject(
                        values + mapOf(
                            "couple_id" to JsonPrimitive(couple.id),
                            "sender_id" to JsonPrimitive(userId),
                            "recipient_id" to JsonPrimitive(couple.partner.id),
                        ),
                    ),
                )
                true
            } else {
                supabase.from(Table)
                    .update(values) {
                        select()
                        filter { eq("id", id) }
                    }
                    .decodeList<ShootingStarDto>()
                    .isNotEmpty()
            }
        }
        val result = when (saved) {
            is DataResult.Failure -> saved
            is DataResult.Success -> if (saved.value) DataResult.Success(Unit) else DataResult.Failure(DataError.StarUnavailable)
        }
        if (result is DataResult.Failure && newPath != null) supabaseCall { bucket.delete(newPath) }
        return result
    }

    override suspend fun delete(id: String): DataResult<Unit> {
        val deleted = supabaseCall {
            supabase.from(Table)
                .delete {
                    select()
                    filter { eq("id", id) }
                }
                .decodeList<ShootingStarDto>()
                .isNotEmpty()
        }
        return when (deleted) {
            is DataResult.Failure -> deleted
            is DataResult.Success -> if (deleted.value) DataResult.Success(Unit) else DataResult.Failure(DataError.StarUnavailable)
        }
    }

    /** RLS returns only the stars that are visible to this user already. */
    private suspend fun fetchReceived(): List<ShootingStar> {
        val userId = currentUserId() ?: return emptyList()
        val result = supabaseCall {
            supabase.from(Table)
                .select {
                    filter { eq("recipient_id", userId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<ShootingStarDto>()
                .map { it.toModel() }
        }
        return (result as? DataResult.Success)?.value.orEmpty()
    }

    private suspend fun fetchRow(id: String): ShootingStarDto? =
        supabase.from(Table).select { filter { eq("id", id) } }.decodeSingleOrNull<ShootingStarDto>()

    private suspend fun upload(coupleId: String, photoUri: String): String {
        val path = "$coupleId/stars/${UUID.randomUUID()}.jpg"
        bucket.upload(path, compressor.compress(photoUri, StarPhotoLongEdge)) { contentType = ContentType.Image.JPEG }
        return path
    }

    private suspend fun signedUrl(path: String): String = bucket.createSignedUrl(path, SignedUrlLifetime)

    private fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    private companion object {
        const val Table = "shooting_stars"
        const val Bucket = "memories"
        val SignedUrlLifetime = 10.minutes
    }
}
