package app.twoverse.core.data

import app.twoverse.core.model.UserLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    /** The user's own latest location, or null when unavailable. */
    val myLocation: Flow<UserLocation?>

    /** The partner's latest location, or null when sharing is off or there is no data. */
    val partnerLocation: Flow<UserLocation?>
}
