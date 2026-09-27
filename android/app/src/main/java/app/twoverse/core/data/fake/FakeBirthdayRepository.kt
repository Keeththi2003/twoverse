package app.twoverse.core.data.fake

import app.twoverse.core.data.BirthdayRepository
import app.twoverse.core.data.local.UserPreferences
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.BirthdayWelcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Sample birthday welcome; whether it was seen is kept on the device so it shows once (FR-BDY-3). */
@Singleton
class FakeBirthdayRepository @Inject constructor(
    private val preferences: UserPreferences,
) : BirthdayRepository {

    override val welcome: Flow<BirthdayWelcome?> =
        preferences.birthdayWelcomeSeen.map { seen -> SampleData.birthdayWelcome.copy(seen = seen) }

    override suspend fun markSeen() {
        preferences.setBirthdayWelcomeSeen()
    }
}
