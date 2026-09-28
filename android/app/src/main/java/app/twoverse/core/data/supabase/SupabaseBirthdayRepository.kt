package app.twoverse.core.data.supabase

import app.twoverse.core.data.BirthdayRepository
import app.twoverse.core.data.CoupleRepository
import app.twoverse.core.data.memory.MemoryPhotoCompressor
import app.twoverse.core.model.BirthdayMessage
import app.twoverse.core.model.BirthdayMessageDraft
import app.twoverse.core.model.BirthdayPhotoChange
import app.twoverse.core.model.BirthdayWelcome
import app.twoverse.core.model.Couple
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/**
 * Birthday welcomes in the birthday_welcomes table, one per recipient and couple (FR-BDY-2).
 * Photos go to the couple's folder in the private "memories" bucket and are shown through
 * short-lived signed URLs; storage policies only allow the author and the recipient.
 */
@Singleton
class SupabaseBirthdayRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val coupleRepository: CoupleRepository,
    private val compressor: MemoryPhotoCompressor,
) : BirthdayRepository {

    /** Re-reads after this user marks the welcome seen. */
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val bucket get() = supabase.storage.from(Bucket)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val welcome: Flow<BirthdayWelcome?> = coupleRepository.couple
        .distinctUntilChanged { old, new -> old?.id == new?.id }
        .flatMapLatest { couple -> if (couple == null) flowOf(null) else receivedWelcome(couple) }
        .distinctUntilChanged()

    override suspend fun markSeen(): DataResult<Unit> = supabaseCall {
        supabase.postgrest.rpc("mark_birthday_welcome_seen")
        refresh.tryEmit(Unit)
        Unit
    }

    override suspend fun myMessage(): DataResult<BirthdayMessage?> {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return DataResult.Failure(DataError.Unknown)
        return supabaseCall {
            val row = supabase.from(Table)
                .select { filter { eq("created_by", userId) } }
                .decodeSingleOrNull<BirthdayWelcomeDto>()
            row?.let {
                BirthdayMessage(
                    message = it.message,
                    showOn = it.showOn?.let(LocalDate::parse),
                    photoUrl = it.photoPath?.let { path -> signedUrl(path) },
                    seen = it.seenAt != null,
                )
            }
        }
    }

    /**
     * Upserts the welcome for the partner. Saving again clears "seen", so an edited or new
     * welcome shows once more (FR-BDY-3). A photo replaced or removed is deleted by the database.
     */
    override suspend fun saveMyMessage(draft: BirthdayMessageDraft): DataResult<Unit> {
        val couple = coupleRepository.couple.first() ?: return DataResult.Failure(DataError.NotPaired)
        val userId = supabase.auth.currentUserOrNull()?.id ?: return DataResult.Failure(DataError.Unknown)
        val newPath = (draft.photo as? BirthdayPhotoChange.Replace)?.let { replace ->
            when (val uploaded = supabaseCall { upload(couple.id, replace.photoUri) }) {
                is DataResult.Success -> uploaded.value
                is DataResult.Failure -> return uploaded
            }
        }
        val saved = supabaseCall {
            supabase.from(Table).upsert(
                buildJsonObject {
                    put("couple_id", couple.id)
                    put("for_user_id", couple.partner.id)
                    put("created_by", userId)
                    put("message", draft.message.trim())
                    draft.showOn?.let { put("show_on", it.toString()) } ?: put("show_on", JsonNull)
                    put("seen_at", JsonNull)
                    when (draft.photo) {
                        BirthdayPhotoChange.Keep -> Unit
                        BirthdayPhotoChange.Remove -> put("photo_path", JsonNull)
                        is BirthdayPhotoChange.Replace -> put("photo_path", newPath)
                    }
                },
            ) { onConflict = "couple_id,for_user_id" }
            Unit
        }
        if (saved is DataResult.Failure && newPath != null) supabaseCall { bucket.delete(newPath) }
        return saved
    }

    private fun receivedWelcome(couple: Couple): Flow<BirthdayWelcome?> = refresh
        .onStart { emit(Unit) }
        .map { (supabaseCall { fetchReceived(couple) } as? DataResult.Success)?.value }

    private suspend fun fetchReceived(couple: Couple): BirthdayWelcome? {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return null
        val row = supabase.from(Table)
            .select { filter { eq("for_user_id", userId) } }
            .decodeSingleOrNull<BirthdayWelcomeDto>()
            ?: return null
        return BirthdayWelcome(
            message = row.message,
            fromName = couple.partner.displayName,
            photoUrl = row.photoPath?.let { signedUrl(it) },
            showOn = row.showOn?.let(LocalDate::parse),
            seen = row.seenAt != null,
        )
    }

    private suspend fun upload(coupleId: String, photoUri: String): String {
        val path = "$coupleId/birthday-${UUID.randomUUID()}.jpg"
        bucket.upload(path, compressor.compress(photoUri)) { contentType = ContentType.Image.JPEG }
        return path
    }

    private suspend fun signedUrl(path: String): String = bucket.createSignedUrl(path, SignedUrlLifetime)

    private companion object {
        const val Table = "birthday_welcomes"
        const val Bucket = "memories"
        val SignedUrlLifetime = 10.minutes
    }
}
