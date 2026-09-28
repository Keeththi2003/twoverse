package app.twoverse.core.data.local

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import app.twoverse.core.model.Couple
import app.twoverse.core.model.CoupleStatus
import app.twoverse.core.model.LocationPrecision
import app.twoverse.core.model.LocationSharing
import app.twoverse.core.model.Memory
import app.twoverse.core.model.MemorySender
import app.twoverse.core.model.Reunion
import app.twoverse.core.model.UserLocation
import app.twoverse.core.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** [OfflineCache] as a JSON file in app-internal storage that is excluded from backups. */
@Singleton
class DataStoreOfflineCache @Inject constructor(
    @ApplicationContext context: Context,
) : OfflineCache {

    private val dataStore: DataStore<CachedSnapshot?> = DataStoreFactory.create(
        serializer = OfflineSnapshotSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { null },
        produceFile = { File(context.noBackupFilesDir, FileName) },
    )

    override val snapshot: Flow<OfflineSnapshot?> = dataStore.data
        .map { it?.toModel() }
        .distinctUntilChanged()

    override suspend fun update(ownerId: String, change: (OfflineSnapshot) -> OfflineSnapshot) {
        dataStore.updateData { saved -> change(saved?.toModel().forOwner(ownerId)).toCached() }
    }

    override suspend fun clear() {
        dataStore.updateData { null }
    }

    private companion object {
        const val FileName = "offline_cache.json"
    }
}

/** Reads and writes the snapshot as JSON; an empty file means nothing is saved. */
internal object OfflineSnapshotSerializer : Serializer<CachedSnapshot?> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: CachedSnapshot? = null

    override suspend fun readFrom(input: InputStream): CachedSnapshot? {
        val text = input.readBytes().decodeToString()
        if (text.isBlank()) return null
        return try {
            json.decodeFromString(CachedSnapshot.serializer(), text)
        } catch (e: SerializationException) {
            throw CorruptionException("Unreadable offline cache", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Unreadable offline cache", e)
        }
    }

    override suspend fun writeTo(t: CachedSnapshot?, output: OutputStream) {
        if (t != null) output.write(json.encodeToString(CachedSnapshot.serializer(), t).encodeToByteArray())
    }
}

@Serializable
internal data class CachedSnapshot(
    val ownerId: String,
    val couple: CachedCouple? = null,
    val sharing: CachedSharing? = null,
    val myLocation: CachedLocation? = null,
    val partnerLocation: CachedLocation? = null,
    val reunion: CachedReunion? = null,
    val memories: List<CachedMemory>? = null,
) {
    fun toModel() = OfflineSnapshot(
        ownerId = ownerId,
        couple = couple?.toModel(),
        sharing = sharing?.toModel(),
        myLocation = myLocation?.toModel(),
        partnerLocation = partnerLocation?.toModel(),
        reunion = reunion?.toModel(),
        memories = memories?.map { it.toModel() },
    )
}

internal fun OfflineSnapshot.toCached() = CachedSnapshot(
    ownerId = ownerId,
    couple = couple?.let { CachedCouple(it.id, it.partner.id, it.partner.displayName, it.connectedAt?.toString()) },
    sharing = sharing?.let { CachedSharing(it.enabled, it.precision.name) },
    myLocation = myLocation?.toCached(),
    partnerLocation = partnerLocation?.toCached(),
    reunion = reunion?.let {
        CachedReunion(it.meetAt.toString(), it.hasTime, it.place, it.note, it.dateSetAt.toString())
    },
    memories = memories?.map {
        CachedMemory(
            id = it.id,
            fromPartner = it.sender == MemorySender.Partner,
            caption = it.caption,
            createdAt = it.createdAt.toString(),
            expiresAt = it.expiresAt?.toString(),
            allowKeep = it.allowKeep,
            viewedAt = it.viewedAt?.toString(),
        )
    },
)

@Serializable
internal data class CachedCouple(
    val id: String,
    val partnerId: String,
    val partnerName: String,
    val connectedAt: String? = null,
) {
    fun toModel() = Couple(
        id = id,
        partner = UserProfile(id = partnerId, displayName = partnerName),
        status = CoupleStatus.Active,
        connectedAt = connectedAt?.let(Instant::parse),
    )
}

@Serializable
internal data class CachedSharing(val enabled: Boolean, val precision: String) {
    fun toModel() = LocationSharing(enabled = enabled, precision = precision.toPrecision())
}

@Serializable
internal data class CachedLocation(
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val precision: String,
    val updatedAt: String,
) {
    fun toModel() = UserLocation(
        userId = userId,
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        precision = precision.toPrecision(),
        city = null,
        updatedAt = Instant.parse(updatedAt),
    )
}

private fun UserLocation.toCached() =
    CachedLocation(userId, latitude, longitude, accuracyMeters, precision.name, updatedAt.toString())

@Serializable
internal data class CachedReunion(
    val meetAt: String,
    val hasTime: Boolean,
    val place: String? = null,
    val note: String? = null,
    val dateSetAt: String,
) {
    fun toModel() = Reunion(
        meetAt = Instant.parse(meetAt),
        hasTime = hasTime,
        place = place,
        note = note,
        dateSetAt = Instant.parse(dateSetAt),
    )
}

@Serializable
internal data class CachedMemory(
    val id: String,
    val fromPartner: Boolean,
    val caption: String? = null,
    val createdAt: String,
    val expiresAt: String? = null,
    val allowKeep: Boolean,
    val viewedAt: String? = null,
) {
    fun toModel() = Memory(
        id = id,
        sender = if (fromPartner) MemorySender.Partner else MemorySender.Me,
        imageUrl = null,
        caption = caption,
        createdAt = Instant.parse(createdAt),
        expiresAt = expiresAt?.let(Instant::parse),
        allowKeep = allowKeep,
        viewedAt = viewedAt?.let(Instant::parse),
    )
}

private fun String.toPrecision(): LocationPrecision =
    LocationPrecision.entries.firstOrNull { it.name == this } ?: LocationPrecision.Approximate
