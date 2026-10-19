package br.com.carvalho.podcast.domain.model

/** A part of an episode (19.1), from the feed's JSON chapters or the audio's own ID3 `CHAP` frames. */
data class Chapter(val startMs: Long, val title: String)

/** The chapter playing at [positionMs]: the last one that already started. */
fun List<Chapter>.at(positionMs: Long): Chapter? = lastOrNull { it.startMs <= positionMs }
