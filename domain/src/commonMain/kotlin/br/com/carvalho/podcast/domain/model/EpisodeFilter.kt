package br.com.carvalho.podcast.domain.model

/** Which of a podcast's episodes a list shows; applied by the database query, not in memory. */
enum class EpisodeFilter { ALL, UNPLAYED, DOWNLOADED }
