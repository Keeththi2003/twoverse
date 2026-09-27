package app.twoverse.core.data

import app.twoverse.core.model.DataResult
import app.twoverse.core.model.PartnerPush

interface PushRepository {
    /** Saves this device's push token for the signed-in user; [token] is a refreshed one if given. */
    suspend fun registerThisDevice(token: String? = null): DataResult<Unit>

    /** Removes this device's token, before signing out, so the user stops getting pushes here. */
    suspend fun unregisterThisDevice(): DataResult<Unit>

    /** Sends a push to the partner only; the server checks the couple and rate limits. */
    suspend fun sendToPartner(push: PartnerPush): DataResult<Unit>
}
