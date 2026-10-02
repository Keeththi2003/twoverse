package app.twoverse.core.model

/** Outcome of a data operation. Repositories never leak backend exceptions (CLAUDE.md, Data layer). */
sealed interface DataResult<out T> {
    data class Success<T>(val value: T) : DataResult<T>

    data class Failure(val error: DataError) : DataResult<Nothing>
}

/** What went wrong, in terms the UI can explain (SRS section 7). */
enum class DataError {
    Network,
    InvalidCredentials,
    EmailNotConfirmed,
    EmailInUse,
    WeakPassword,
    RateLimited,
    InvalidCoupleCode,
    AlreadyPaired,
    NotPaired,

    /** The memory expired, was deleted or hidden (BR-6). */
    MemoryUnavailable,

    /** The Shooting Star was deleted, already seen, or isn't visible yet (FR-STAR-15). */
    StarUnavailable,

    /** The 7-day window to reconnect has passed (SRS 12). */
    ReconnectUnavailable,
    Unknown,
}
