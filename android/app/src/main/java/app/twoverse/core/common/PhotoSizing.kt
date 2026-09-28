package app.twoverse.core.common

import kotlin.math.max
import kotlin.math.roundToInt

/** Memory photos are sent with their long edge at most this many pixels (FR-MEM-2). */
const val MemoryPhotoLongEdge = 1080

data class PixelSize(val width: Int, val height: Int)

/**
 * The size a [width] × [height] photo is scaled to so its long edge fits [maxLongEdge], keeping
 * the aspect ratio. Smaller photos keep their size; they are never enlarged.
 */
fun scaledToLongEdge(width: Int, height: Int, maxLongEdge: Int = MemoryPhotoLongEdge): PixelSize {
    require(width > 0 && height > 0) { "Photo size must be positive" }
    val longEdge = max(width, height)
    if (longEdge <= maxLongEdge) return PixelSize(width, height)
    val scale = maxLongEdge.toDouble() / longEdge
    return PixelSize(
        width = (width * scale).roundToInt().coerceIn(1, maxLongEdge),
        height = (height * scale).roundToInt().coerceIn(1, maxLongEdge),
    )
}
