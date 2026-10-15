package br.com.carvalho.podcast.domain.player

import br.com.carvalho.podcast.domain.model.Chapter
import br.com.carvalho.podcast.domain.model.Episode

/** Finds an episode's chapters (19.1), in start order; none when it has none or they cannot be read. */
fun interface ChapterSource {
    suspend fun chapters(episode: Episode): List<Chapter>
}
