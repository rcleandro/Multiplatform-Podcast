package br.com.carvalho.podcast.domain.model

/** Filters of the Episodes tab, in the order the tab shows them (ADR 0005). Applied by the database query. */
enum class EpisodeListFilter { NEW, ALL, IN_PROGRESS, SAVED, DOWNLOADED, HISTORY }
