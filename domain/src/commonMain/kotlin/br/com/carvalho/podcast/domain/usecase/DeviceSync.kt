package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.SyncPlan
import br.com.carvalho.podcast.domain.model.SyncSnapshot
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Changes come in bursts (progress every few seconds while playing): send once they settle, as WearableSync does. */
private const val SETTLE_SECONDS = 10

/**
 * One side of the iPhone ↔ Apple Watch sync (25.14d), as the Wear OS one (19.9b): this device's [SyncSnapshot] as
 * JSON once its changes settle, and the other device's merged by [LibrarySyncUseCase]. WatchConnectivity, in Swift on
 * both sides, only carries the text.
 */
class DeviceSync(private val sync: LibrarySyncUseCase, private val settle: Duration = SETTLE_SECONDS.seconds) {
    private val json = Json { ignoreUnknownKeys = true }

    /** This device's snapshot each time it settles on something new. */
    @OptIn(FlowPreview::class)
    fun outgoing(): Flow<String> = sync.changes()
        .debounce(settle)
        .map { json.encodeToString(SyncSnapshot.serializer(), sync.snapshot()) }
        .distinctUntilChanged()

    /** Merges the other device's snapshot; a broken one is left out, and says so with null. */
    suspend fun receive(text: String): SyncPlan? = decode(text)?.let { sync.apply(it) }

    private fun decode(text: String): SyncSnapshot? = try {
        json.decodeFromString(SyncSnapshot.serializer(), text)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
