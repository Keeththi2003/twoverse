package app.twoverse.core.data.fake

import app.twoverse.core.data.BirthdayRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.BirthdayWelcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeBirthdayRepository @Inject constructor() : BirthdayRepository {
    private val current = MutableStateFlow<BirthdayWelcome?>(SampleData.birthdayWelcome)

    override val welcome: StateFlow<BirthdayWelcome?> = current

    override suspend fun markSeen() {
        current.update { it?.copy(seen = true) }
    }
}
