package app.twoverse.testing

import app.twoverse.core.model.ShootingStar
import app.twoverse.core.model.StarContent
import app.twoverse.core.model.StarLayout
import java.time.Instant

/** A Shooting Star for tests; by default visible (sent for the next open) and unseen. */
fun testStar(
    id: String = "star-1",
    content: StarContent = StarContent(layout = StarLayout.PhotoMessage, title = "Happy Birthday", message = "Hi"),
    hasPhoto: Boolean = false,
    showAt: Instant? = null,
    seenAt: Instant? = null,
    createdAt: Instant = Instant.parse("2026-09-20T09:00:00Z"),
) = ShootingStar(
    id = id,
    content = content,
    hasPhoto = hasPhoto,
    showAt = showAt,
    seenAt = seenAt,
    createdAt = createdAt,
)
