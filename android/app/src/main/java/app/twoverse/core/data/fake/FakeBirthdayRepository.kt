package app.twoverse.core.data.fake

import app.twoverse.core.data.BirthdayRepository
import app.twoverse.core.model.BirthdayMessage
import app.twoverse.core.model.BirthdayMessageDraft
import app.twoverse.core.model.BirthdayPhotoChange
import app.twoverse.core.model.BirthdayWelcome
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** In-memory birthday welcomes for previews and tests; starts with none. */
class FakeBirthdayRepository @Inject constructor() : BirthdayRepository {
    private val received = MutableStateFlow<BirthdayWelcome?>(null)
    private var written: BirthdayMessage? = null
    private var nextFailure: DataError? = null

    override val welcome: StateFlow<BirthdayWelcome?> = received

    /** Every draft saved, for tests. */
    val savedDrafts = mutableListOf<BirthdayMessageDraft>()

    fun setWelcome(welcome: BirthdayWelcome?) {
        received.value = welcome
    }

    fun setMyMessage(message: BirthdayMessage?) {
        written = message
    }

    /** Makes the next call fail with [error], to test error handling. */
    fun failNextWith(error: DataError) {
        nextFailure = error
    }

    override suspend fun markSeen(): DataResult<Unit> = respond {
        received.value = received.value?.copy(seen = true)
    }

    override suspend fun myMessage(): DataResult<BirthdayMessage?> = respond { written }

    override suspend fun saveMyMessage(draft: BirthdayMessageDraft): DataResult<Unit> = respond {
        savedDrafts += draft
        val photoUrl = when (val photo = draft.photo) {
            BirthdayPhotoChange.Keep -> written?.photoUrl
            BirthdayPhotoChange.Remove -> null
            is BirthdayPhotoChange.Replace -> photo.photoUri
        }
        written = BirthdayMessage(message = draft.message, showOn = draft.showOn, photoUrl = photoUrl, seen = false)
    }

    private fun <T> respond(action: () -> T): DataResult<T> {
        val error = nextFailure
        nextFailure = null
        return if (error != null) DataResult.Failure(error) else DataResult.Success(action())
    }
}
