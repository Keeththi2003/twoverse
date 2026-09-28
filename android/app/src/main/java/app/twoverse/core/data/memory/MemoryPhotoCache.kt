package app.twoverse.core.data.memory

import android.content.Context
import androidx.core.net.toUri
import app.twoverse.core.common.isExpired
import app.twoverse.core.model.Memory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory photos kept on this device, one file per memory, so each is downloaded once
 * (NFR-PRF-3). Files live in app-internal storage that is excluded from backups and never
 * shared with the gallery (FR-VLT-7, BR-5).
 */
@Singleton
class MemoryPhotoCache @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val directory = File(context.noBackupFilesDir, DirectoryName)
    private val mutex = Mutex()

    /** Null until the directory has been read. */
    private val ids = MutableStateFlow<Set<String>?>(null)

    /** Ids of the memories whose photo is on this device. */
    val cachedIds: Flow<Set<String>> = flow {
        ensureLoaded()
        emitAll(ids.filterNotNull())
    }

    /** What the UI loads the photo of memory [id] from. */
    fun uriFor(id: String): String = fileFor(id).toUri().toString()

    /** Stores a photo this user just sent, so it is never downloaded again. */
    suspend fun save(id: String, bytes: ByteArray) {
        write(id) { it.writeBytes(bytes) }
    }

    /** Downloads the photo from a short-lived signed [url] (NFR-SEC-2). */
    suspend fun download(id: String, url: String) {
        write(id) { file ->
            runInterruptible {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = TimeoutMillis
                    connection.readTimeout = TimeoutMillis
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw IOException("Photo download failed with HTTP ${connection.responseCode}")
                    }
                    connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    /** Removes the photos of deleted, hidden or expired memories (FR-DEL-4). */
    suspend fun remove(remove: Set<String>) {
        if (remove.isEmpty()) return
        withContext(Dispatchers.IO) { remove.forEach { fileFor(it).delete() } }
        ids.update { it.orEmpty() - remove }
    }

    suspend fun clear() {
        ensureLoaded()
        remove(ids.value.orEmpty())
    }

    /** Writes to a temporary file first, so a failed download never looks cached. */
    private suspend fun write(id: String, writeTo: suspend (File) -> Unit) {
        ensureLoaded()
        withContext(Dispatchers.IO) {
            directory.mkdirs()
            val temp = File(directory, "${fileName(id)}$TempSuffix")
            try {
                writeTo(temp)
                if (!temp.renameTo(fileFor(id))) throw IOException("Couldn't store photo")
            } finally {
                temp.delete()
            }
        }
        ids.update { it.orEmpty() + id }
    }

    private suspend fun ensureLoaded() {
        if (ids.value != null) return
        mutex.withLock {
            if (ids.value != null) return
            ids.value = withContext(Dispatchers.IO) {
                directory.listFiles().orEmpty()
                    .filter { it.name.endsWith(Extension) }
                    .map { it.name.removeSuffix(Extension) }
                    .toSet()
            }
        }
    }

    private fun fileFor(id: String) = File(directory, fileName(id))

    /** Memory ids are UUIDs; anything else is rejected so it can't point outside the directory. */
    private fun fileName(id: String): String = UUID.fromString(id).toString() + Extension

    private companion object {
        const val DirectoryName = "memories"
        const val Extension = ".jpg"
        const val TempSuffix = ".part"
        const val TimeoutMillis = 30_000
    }
}

/** Cached photos whose memory is gone: deleted, hidden, expired or no longer shared (FR-DEL-4). */
internal fun stalePhotos(cachedIds: Set<String>, memories: List<Memory>, now: Instant): Set<String> =
    cachedIds - memories.filterNot { isExpired(it.expiresAt, now) }.map { it.id }.toSet()
