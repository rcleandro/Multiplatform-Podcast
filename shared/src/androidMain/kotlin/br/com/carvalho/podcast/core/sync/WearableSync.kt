package br.com.carvalho.podcast.core.sync

import android.content.Context
import android.net.Uri
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.SyncSnapshot
import br.com.carvalho.podcast.domain.usecase.LibrarySyncUseCase
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import org.koin.mp.KoinPlatform.getKoin
import kotlin.time.Duration.Companion.seconds

private const val TAG = "WearableSync"
private const val PATH = "/library-sync"
private const val KEY = "snapshot"
private const val SETTLE_SECONDS = 10

/** Changes come in bursts (progress every few seconds while playing): send once they settle. */
private val SETTLE = SETTLE_SECONDS.seconds

/**
 * Phone ↔ watch library sync (19.9b) over the Wear OS Data Layer: Bluetooth when near, Google's cloud otherwise, both
 * encrypted, so private feed addresses may travel. Each device keeps its own [SyncSnapshot] as a data item; the
 * other's arrives in [WearableSyncService] and is merged by [LibrarySyncUseCase]. Without Wear OS on the phone the
 * Data Layer is missing and every call fails quietly.
 */
object WearableSync {
    private val json = Json { ignoreUnknownKeys = true }

    /** Outlives the listener service, which Android may stop as soon as the event is handed over. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val sync: LibrarySyncUseCase get() = getKoin().get()

    /** Publishes this device's snapshot whenever it changes, and first takes what the other side sent while closed. */
    @OptIn(FlowPreview::class)
    fun keepInSync(context: Context, appScope: CoroutineScope) {
        val app = context.applicationContext
        appScope.launch { safely("catch up") { receiveAll(app) } }
        sync.changes()
            .debounce(SETTLE)
            .map { sync.snapshot() }
            .distinctUntilChanged()
            .onEach { safely("publish") { publish(app, it) } }
            .launchIn(appScope)
    }

    internal fun onDataChanged(context: Context, events: DataEventBuffer) {
        // The buffer is released when the callback returns: freeze what is needed first.
        val items = events.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == PATH }
            .map { it.dataItem.freeze() }
        if (items.isEmpty()) return
        val app = context.applicationContext
        scope.launch {
            safely("receive") {
                val here = Wearable.getNodeClient(app).localNode.await().id
                items.filter { it.uri.host != here }.forEach { receive(app, it) }
            }
        }
    }

    private suspend fun publish(context: Context, snapshot: SyncSnapshot) {
        val bytes = json.encodeToString(SyncSnapshot.serializer(), snapshot).encodeToByteArray()
        // An asset, not a plain value: a data item holds at most 100 KB, and a long history is more.
        val request = PutDataMapRequest.create(PATH).apply { dataMap.putAsset(KEY, Asset.createFromBytes(bytes)) }
        Wearable.getDataClient(context).putDataItem(request.asPutDataRequest()).await()
        AppLogger.i(TAG, "Published ${snapshot.podcasts.size} podcasts and ${snapshot.episodes.size} episodes")
    }

    private suspend fun receiveAll(context: Context) {
        val here = Wearable.getNodeClient(context).localNode.await().id
        val buffer = Wearable.getDataClient(context).getDataItems(Uri.parse("wear://*$PATH")).await()
        val items = try {
            buffer.map { it.freeze() }
        } finally {
            buffer.release()
        }
        items.filter { it.uri.host != here }.forEach { receive(context, it) }
    }

    private suspend fun receive(context: Context, item: DataItem) {
        val asset = DataMapItem.fromDataItem(item).dataMap.getAsset(KEY) ?: return
        val bytes = Wearable.getDataClient(context).getFdForAsset(asset).await().inputStream.use { it.readBytes() }
        val remote = json.decodeFromString(SyncSnapshot.serializer(), bytes.decodeToString())
        val plan = sync.apply(remote)
        AppLogger.i(
            TAG,
            "Took ${plan.subscribe.size} podcasts, removed ${plan.unsubscribe.size}, " +
                "${plan.episodes.size} episodes and ${if (plan.queue == null) "no" else "the"} queue",
        )
    }

    @Suppress("TooGenericExceptionCaught") // the Data Layer fails with ApiException, IOException and more
    private suspend fun safely(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            AppLogger.i(TAG, "Could not $what: ${e::class.simpleName}")
        }
    }
}

/** Wakes the app when the other device's snapshot changes, even with the app closed. */
class WearableSyncService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) = WearableSync.onDataChanged(this, dataEvents)
}
