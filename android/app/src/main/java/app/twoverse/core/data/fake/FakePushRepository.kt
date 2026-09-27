package app.twoverse.core.data.fake

import app.twoverse.core.data.PushRepository
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.PartnerPush
import javax.inject.Inject

/** Records push calls for tests. */
class FakePushRepository @Inject constructor() : PushRepository {
    /** Every call in order, e.g. "register:token", "unregister", "send:WakeUp". */
    val calls = mutableListOf<String>()

    override suspend fun registerThisDevice(token: String?): DataResult<Unit> {
        calls += "register:${token ?: "current"}"
        return DataResult.Success(Unit)
    }

    override suspend fun unregisterThisDevice(): DataResult<Unit> {
        calls += "unregister"
        return DataResult.Success(Unit)
    }

    override suspend fun sendToPartner(push: PartnerPush): DataResult<Unit> {
        calls += "send:${push.name}"
        return DataResult.Success(Unit)
    }
}
