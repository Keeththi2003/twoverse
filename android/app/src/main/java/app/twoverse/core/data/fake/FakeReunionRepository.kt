package app.twoverse.core.data.fake

import app.twoverse.core.data.ReunionRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.Reunion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeReunionRepository @Inject constructor() : ReunionRepository {
    private val current = MutableStateFlow<Reunion?>(SampleData.reunion)

    override val reunion: StateFlow<Reunion?> = current

    override suspend fun setReunion(reunion: Reunion) {
        current.value = reunion
    }

    override suspend fun clearReunion() {
        current.value = null
    }
}
