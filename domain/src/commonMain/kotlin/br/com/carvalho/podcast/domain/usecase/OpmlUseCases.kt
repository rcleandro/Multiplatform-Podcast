package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.domain.repository.OpmlFormat
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/** An OPML file and how many podcasts it lists. */
data class OpmlExport(val opml: String, val podcasts: Int)

/** The library as an OPML file (18.13), by title. */
class ExportOpmlUseCase(private val repository: PodcastRepository, private val format: OpmlFormat) {
    suspend operator fun invoke(): OpmlExport {
        val podcasts = repository.getLibrary().first().map { it.podcast }
        return OpmlExport(format.write(podcasts), podcasts.size)
    }
}

/** What an OPML import did: podcasts added, already in the library, and feeds that could not be read. */
data class OpmlImport(val added: Int, val known: Int, val failed: Int)

/**
 * Subscribes to every feed an OPML file lists (18.13), the same way "Add by feed address" does, so a feed already in
 * the library under another spelling is not added twice. [onProgress] gets how many are done out of how many.
 */
class ImportOpmlUseCase(private val format: OpmlFormat, private val addPodcast: AddPodcastFromUrlUseCase) {
    suspend operator fun invoke(opml: String, onProgress: (Int, Int) -> Unit = { _, _ -> }): OpmlImport {
        val feeds = format.read(opml)
        var done = 0
        onProgress(done, feeds.size)
        // ponytail: a few feeds at a time in fixed batches; a slow feed holds its batch back. A semaphore if that
        // shows up in imports of hundreds of feeds.
        val results = feeds.chunked(FEEDS_AT_ONCE).flatMap { batch ->
            coroutineScope { batch.map { async { addPodcast(it) } }.awaitAll() }.also {
                done += it.size
                onProgress(done, feeds.size)
            }
        }
        return OpmlImport(
            added = results.count { it.isSuccess },
            known = results.count { it.exceptionOrNull() == AppError.AlreadyExists },
            failed = results.count { it.isFailure && it.exceptionOrNull() != AppError.AlreadyExists },
        )
    }

    private companion object {
        const val FEEDS_AT_ONCE = 4
    }
}
