package br.com.carvalho.podcast.core.sync

import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.FolderSyncUseCase
import br.com.carvalho.podcast.domain.usecase.LibrarySyncUseCase
import br.com.carvalho.podcast.domain.usecase.SyncFiles
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.fromBookmarkData
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import org.koin.mp.KoinPlatform.getKoin
import kotlin.io.encoding.Base64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val SETTLE_SECONDS = 10
private const val LOOK_AGAIN_MINUTES = 5

/** Changes come in bursts (progress every few seconds while playing): write once they settle. */
private val SETTLE = SETTLE_SECONDS.seconds

/** How often what other devices wrote is looked for while the app runs. */
private val LOOK_AGAIN = LOOK_AGAIN_MINUTES.minutes

/**
 * Library sync through a folder (19.12, ADR 0011): exchanges with the other devices' files when a folder is chosen,
 * every few minutes and when the library settles after a change, while the app runs.
 */
object FolderSync {
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun keepInSync(scope: CoroutineScope) {
        val preferences = getKoin().get<PreferencesRepository>()
        val sync = getKoin().get<LibrarySyncUseCase>()
        val folderSync = getKoin().get<FolderSyncUseCase>()
        preferences.syncFolder
            .flatMapLatest { folder ->
                val due = merge(every(LOOK_AGAIN), sync.changes().drop(1).debounce(SETTLE))
                if (folder == null) emptyFlow() else due.map { folder }
            }
            .onEach { folder -> folderSync.exchange(FolderFiles(folder.bookmark), folder.fileName) }
            .flowOn(Dispatchers.IO)
            .launchIn(scope)
    }

    private fun every(period: Duration): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(period)
        }
    }
}

/** The folder behind a bookmark: a content URI from the system picker on Android, a path on Desktop. */
internal class FolderFiles(bookmark: String) : SyncFiles {
    private val folder by lazy { PlatformFile.fromBookmarkData(Base64.decode(bookmark)) }

    override suspend fun read(): Map<String, String> = folder.list()
        // Whoever can write to the folder controls these files (T12 of ADR 0014): one too big is skipped, not read.
        .filter { it.isRegularFile() && FolderSyncUseCase.isSnapshotFile(it.name) }
        .filter { it.size() <= AppConfig.MAX_TEXT_BYTES }
        .associate { it.name to it.readString() }

    override suspend fun write(name: String, text: String) = (folder / name).writeString(text)
}
