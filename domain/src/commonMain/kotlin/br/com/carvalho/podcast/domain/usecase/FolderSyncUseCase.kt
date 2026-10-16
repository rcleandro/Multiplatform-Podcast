package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.domain.model.SyncSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.random.Random

private const val TAG = "FolderSync"
private const val FILE_PREFIX = "podcast-sync-"
private const val FILE_EXTENSION = ".json"
private const val HEX = 16

/** The snapshot files of a sync folder (19.12); the platform reads and writes them. */
interface SyncFiles {
    /** The folder's snapshot files ([FolderSyncUseCase.isSnapshotFile]), text by name. */
    suspend fun read(): Map<String, String>

    suspend fun write(name: String, text: String)
}

sealed interface FolderSyncStatus {
    data object Idle : FolderSyncStatus
    data class Synced(val at: Long) : FolderSyncStatus

    /** The folder could not be read or written: gone, or the permission to it taken back. */
    data object Failed : FolderSyncStatus
}

/**
 * Library sync through a folder the user's cloud or sync app carries between devices (19.12, ADR 0011). Each device
 * writes only its own snapshot file and takes from everyone else's with the 19.9b merge, so two devices never write the
 * same file and a sync app never has a conflict to resolve. Files of devices no longer in use stay, and lose to newer
 * changes as any other snapshot does.
 */
class FolderSyncUseCase(
    private val sync: LibrarySyncUseCase,
    private val now: () -> Long = ::getCurrentTimestamp,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val state = MutableStateFlow<FolderSyncStatus>(FolderSyncStatus.Idle)
    val status: StateFlow<FolderSyncStatus> = state.asStateFlow()

    /** Takes what the other devices' files know newer, then writes this device's file to [ownName] if it changed. */
    @Suppress("TooGenericExceptionCaught") // each platform's files fail with their own exceptions
    suspend fun exchange(files: SyncFiles, ownName: String) {
        try {
            val found = files.read().filterKeys(::isSnapshotFile)
            found.filterKeys { it != ownName }.values.forEach { text -> decode(text)?.let { sync.apply(it) } }
            val own = json.encodeToString(SyncSnapshot.serializer(), sync.snapshot())
            if (found[ownName] != own) files.write(ownName, own)
            AppLogger.i(TAG, "Synced with ${found.size - (if (ownName in found) 1 else 0)} other devices")
            state.value = FolderSyncStatus.Synced(now())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.i(TAG, "Could not sync with the folder: ${e::class.simpleName}")
            state.value = FolderSyncStatus.Failed
        }
    }

    /** A file the sync app is still bringing in may be cut short: it waits for the next exchange. */
    private fun decode(text: String): SyncSnapshot? = try {
        json.decodeFromString(SyncSnapshot.serializer(), text)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    companion object {
        /** The folder is the user's: only files this app wrote are read. */
        fun isSnapshotFile(name: String): Boolean = name.startsWith(FILE_PREFIX) && name.endsWith(FILE_EXTENSION)

        /** A new device's file: random, so two devices never pick the same one. */
        fun newFileName(): String = FILE_PREFIX + Random.nextLong().toULong().toString(radix = HEX) + FILE_EXTENSION
    }
}
