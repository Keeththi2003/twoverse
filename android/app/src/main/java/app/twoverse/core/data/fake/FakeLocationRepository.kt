package app.twoverse.core.data.fake

import app.twoverse.core.data.LocationRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.UserLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeLocationRepository @Inject constructor() : LocationRepository {
    override val myLocation: StateFlow<UserLocation?> = MutableStateFlow(SampleData.myLocation)

    override val partnerLocation: StateFlow<UserLocation?> = MutableStateFlow(SampleData.partnerLocation)
}
