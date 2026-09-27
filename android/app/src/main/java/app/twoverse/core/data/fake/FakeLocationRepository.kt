package app.twoverse.core.data.fake

import app.twoverse.core.common.forPrecision
import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DevicePosition
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.LocationSharing
import app.twoverse.core.model.UserLocation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import java.time.Instant
import javax.inject.Inject

/**
 * Sample locations for previews and tests. Sharing starts on; the partner "reports" a fresh
 * position every 15 s. Tests can set [failNextWith] or replace [partner].
 */
class FakeLocationRepository @Inject constructor() : LocationRepository {
    private val mine = MutableStateFlow<UserLocation?>(SampleData.myLocation)
    private val sharingState = MutableStateFlow(LocationSharing(enabled = true))

    /** The partner's position; null means they don't share it. */
    val partner = MutableStateFlow<UserLocation?>(SampleData.partnerLocation)

    /** The next call fails with this error (then it resets). */
    var failNextWith: DataError? = null

    override val myLocation: StateFlow<UserLocation?> = mine

    override val partnerLocation: Flow<UserLocation?> = flow {
        while (true) {
            emit(partner.value?.copy(updatedAt = Instant.now()))
            delay(PartnerUpdateMillis)
        }
    }

    override val sharing: StateFlow<LocationSharing> = sharingState

    override suspend fun setSharingEnabled(enabled: Boolean): DataResult<Unit> = respond {
        sharingState.update { it.copy(enabled = enabled) }
        if (!enabled) mine.value = null
    }

    override suspend fun setPrecision(precision: LocationPrecision): DataResult<Unit> = respond {
        sharingState.update { it.copy(precision = precision) }
    }

    override suspend fun upload(position: DevicePosition): DataResult<Unit> = respond {
        val sharing = sharingState.value
        if (!sharing.enabled) return@respond
        val stored = position.forPrecision(sharing.precision)
        mine.value = SampleData.myLocation.copy(
            latitude = stored.latitude,
            longitude = stored.longitude,
            accuracyMeters = stored.accuracyMeters,
            precision = sharing.precision,
            updatedAt = Instant.now(),
        )
    }

    private fun <T> respond(action: () -> T): DataResult<T> {
        val error = failNextWith
        failNextWith = null
        return if (error != null) DataResult.Failure(error) else DataResult.Success(action())
    }

    private companion object {
        const val PartnerUpdateMillis = 15_000L
    }
}
