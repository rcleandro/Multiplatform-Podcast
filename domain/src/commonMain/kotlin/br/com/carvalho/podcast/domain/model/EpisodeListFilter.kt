package br.com.carvalho.podcast.domain.model

/** Filters of the Episodes tab (ADR 0005); "new" arrives with 18.7. Applied by the database query. */
enum class EpisodeListFilter { ALL, IN_PROGRESS, DOWNLOADED }
