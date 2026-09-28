package app.twoverse.core.data.memory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.core.net.toUri
import app.twoverse.core.common.scaledToLongEdge
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Prepares a picked photo for upload (FR-MEM-2): decoded upright, scaled so its long edge is at
 * most 1080 px and re-encoded as JPEG. Re-encoding also drops metadata such as where it was taken.
 */
class MemoryPhotoCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun compress(photoUri: String): ByteArray = withContext(Dispatchers.IO) {
        val source = ImageDecoder.createSource(context.contentResolver, photoUri.toUri())
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val target = scaledToLongEdge(info.size.width, info.size.height)
            decoder.setTargetSize(target.width, target.height)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        try {
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JpegQuality, out)
                out.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        const val JpegQuality = 85
    }
}
