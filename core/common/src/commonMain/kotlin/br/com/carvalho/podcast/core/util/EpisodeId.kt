package br.com.carvalho.podcast.core.util

import okio.ByteString.Companion.encodeUtf8

private const val ID_LENGTH = 32

/**
 * A stable episode id, unique across feeds: a hash of the podcast and the item's guid, or its audio URL when the
 * feed has no guid. It is also the downloaded file's name, so it must not change between app versions.
 */
fun episodeId(podcastId: String, guid: String?, audioUrl: String): String =
    "$podcastId\n${guid ?: audioUrl}".encodeUtf8().sha256().hex().take(ID_LENGTH)
