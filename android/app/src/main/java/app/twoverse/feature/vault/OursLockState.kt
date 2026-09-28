package app.twoverse.feature.vault

import app.twoverse.core.data.SettingsRepository
import app.twoverse.core.data.local.OursLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** True while Ours must stay hidden: Lock Ours is on and this session isn't unlocked (FR-VLT-5). */
internal fun oursLocked(settingsRepository: SettingsRepository, oursLock: OursLock): Flow<Boolean> =
    combine(settingsRepository.settings, oursLock.isUnlocked) { settings, unlocked -> settings.lockOurs && !unlocked }
        .distinctUntilChanged()
