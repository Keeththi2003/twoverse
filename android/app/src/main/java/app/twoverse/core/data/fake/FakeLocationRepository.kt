package app.twoverse.core.data.fake

import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.UserLocation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** The partner's phone "reports" a fresh location every 15 s while the app is open, as with FR-LOC-3. */
@Singleton
class FakeLocationRepository @Inject constructor() : LocationRepository {
    override val myLocation: StateFlow<UserLocation?> = MutableStateFlow(SampleData.myLocation)

    override val partnerLocation: Flow<UserLocation?> = flow {
        while (true) {
            emit(SampleData.partnerLocation.copy(updatedAt = Instant.now()))
            delay(PartnerUpdateMillis)
        }
    }

    private companion object {
        const val PartnerUpdateMillis = 15_000L
    }
}
