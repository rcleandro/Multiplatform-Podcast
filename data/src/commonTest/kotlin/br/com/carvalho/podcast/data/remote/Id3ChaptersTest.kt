package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.domain.model.Chapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Id3ChaptersTest {

    @Test
    fun readsTheChaptersOfAVersion24TagInStartOrder() {
        val tag = id3Tag(
            4,
            frame(4, "TIT2", byteArrayOf(3) + "Episode".encodeToByteArray()),
            chapterFrame(4, "ch1", 90_000, title(4, "News")),
            chapterFrame(4, "ch0", 0, title(4, "Intro")),
            padding = 64,
        )

        assertEquals(listOf(Chapter(0, "Intro"), Chapter(90_000, "News")), Id3Chapters.parse(tag))
    }

    @Test
    fun readsVersion23SizesAndEveryTextEncoding() {
        val tag = id3Tag(
            3,
            chapterFrame(3, "a", 0, utf16Title(3, "Começo")),
            chapterFrame(3, "b", 1_000, latin1Title(3, "Até já")),
            chapterFrame(3, "c", 2_000),
            extendedHeader = true,
        )

        assertEquals(
            listOf(Chapter(0, "Começo"), Chapter(1_000, "Até já"), Chapter(2_000, "")),
            Id3Chapters.parse(tag),
        )
    }

    @Test
    fun skipsTheExtendedHeaderOfVersion24() {
        val tag = id3Tag(4, chapterFrame(4, "a", 5, title(4, "One")), extendedHeader = true)

        assertEquals(listOf(Chapter(5, "One")), Id3Chapters.parse(tag))
    }

    @Test
    fun theTagLengthCountsItsHeaderAndOnlyVersions23And24Count() {
        val tag = id3Tag(4, chapterFrame(4, "a", 0, title(4, "One")), padding = 10)

        assertEquals(tag.size, Id3Chapters.tagLength(tag.copyOf(Id3Chapters.HEADER_SIZE)))
        assertNull(Id3Chapters.tagLength(id3Tag(2).also { it[3] = 2 }))
        assertNull(Id3Chapters.tagLength("not an mp3".encodeToByteArray()))
    }

    @Test
    fun aFrameRunningPastTheTagEndsTheReading() {
        val whole = id3Tag(4, chapterFrame(4, "a", 0, title(4, "One")), chapterFrame(4, "b", 9, title(4, "Two")))

        assertEquals(listOf(Chapter(0, "One")), Id3Chapters.parse(whole.copyOf(whole.size - 3)))
    }
}
