package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.domain.model.Chapter

/**
 * The chapters an ID3v2.3 or 2.4 tag at the start of an MP3 carries: its `CHAP` frames (the ID3 chapter addendum)
 * with the `TIT2` title inside each. Everything else in the tag is skipped.
 */
// ponytail: ignores the tag-wide unsynchronisation flag, which no current podcast tool sets; undo it if one does.
internal object Id3Chapters {
    const val HEADER_SIZE = 10

    private const val FRAME_HEADER_SIZE = 10
    private const val ID_SIZE = 4
    private const val SIZE_OFFSET = 6
    private const val FLAGS_OFFSET = 5
    private const val VERSION_OFFSET = 3
    private const val V23 = 3
    private const val V24 = 4
    private const val EXTENDED_HEADER = 0x40

    /** Start, end, start offset and end offset, before a chapter's own frames. */
    private const val TIMES_SIZE = 16
    private const val LATIN_1 = 0
    private const val UTF_16 = 1
    private const val UTF_16_BE = 2
    private const val BYTE_MASK = 0xFF
    private const val SYNCSAFE_MASK = 0x7F
    private const val SYNCSAFE_BITS = 7
    private const val UNSIGNED_INT_MASK = 0xFFFFFFFFL

    /** The whole tag's length, header included; `null` when [header] does not start an ID3v2.3 or 2.4 tag. */
    fun tagLength(header: ByteArray): Int? {
        val isTag = header.size >= HEADER_SIZE && header.decodeToString(0, ID_SIZE - 1) == "ID3" &&
            header[VERSION_OFFSET].toInt() in V23..V24
        return if (isTag) HEADER_SIZE + syncsafe(header, SIZE_OFFSET) else null
    }

    fun parse(tag: ByteArray): List<Chapter> {
        val length = tagLength(tag) ?: return emptyList()
        val version = tag[VERSION_OFFSET].toInt()
        var offset = HEADER_SIZE
        if (tag[FLAGS_OFFSET].toInt() and EXTENDED_HEADER != 0) {
            // 2.3 counts the size field out of the extended header's size; 2.4 counts it in.
            offset += if (version == V24) syncsafe(tag, offset) else ID_SIZE + int32(tag, offset)
        }
        return frames(tag, offset, minOf(length, tag.size), version)
            .filter { it.id == "CHAP" }
            .mapNotNull { chapter(tag, it, version) }
            .sortedBy { it.startMs }
    }

    private class Frame(val id: String, val start: Int, val end: Int)

    private fun frames(bytes: ByteArray, from: Int, to: Int, version: Int): List<Frame> {
        val frames = mutableListOf<Frame>()
        var offset = from
        // A zero byte where a frame's name should be is the padding that ends the tag.
        while (offset + FRAME_HEADER_SIZE <= to && bytes[offset] != 0.toByte()) {
            val size = if (version == V24) syncsafe(bytes, offset + ID_SIZE) else int32(bytes, offset + ID_SIZE)
            val start = offset + FRAME_HEADER_SIZE
            if (size < 0 || start + size > to) break
            frames += Frame(bytes.decodeToString(offset, offset + ID_SIZE), start, start + size)
            offset = start + size
        }
        return frames
    }

    /** A null-terminated element ID, the times, then frames of its own; a chapter without a title gets a blank one. */
    private fun chapter(bytes: ByteArray, frame: Frame, version: Int): Chapter? {
        val idEnd = (frame.start until frame.end).firstOrNull { bytes[it] == 0.toByte() }
        val times = idEnd?.plus(1)?.takeIf { it + TIMES_SIZE <= frame.end } ?: return null
        val title = frames(bytes, times + TIMES_SIZE, frame.end, version).firstOrNull { it.id == "TIT2" }
            ?.let { text(bytes, it.start, it.end) }
        return Chapter(startMs = int32(bytes, times).toLong() and UNSIGNED_INT_MASK, title = title.orEmpty())
    }

    /** A text frame: one encoding byte, then the text, which may hold more than one string split by nulls. */
    private fun text(bytes: ByteArray, from: Int, to: Int): String {
        if (from >= to) return ""
        val body = bytes.copyOfRange(from + 1, to)
        val decoded = when (bytes[from].toInt()) {
            LATIN_1 -> CharArray(body.size) { (body[it].toInt() and BYTE_MASK).toChar() }.concatToString()
            UTF_16 -> utf16WithBom(body)
            UTF_16_BE -> utf16(body, bigEndian = true)
            else -> body.decodeToString()
        }
        return decoded.substringBefore('\u0000').trim()
    }

    private fun utf16WithBom(body: ByteArray): String {
        if (body.size < 2) return ""
        val bigEndian = body[0] == 0xFE.toByte() && body[1] == 0xFF.toByte()
        return utf16(body.copyOfRange(2, body.size), bigEndian)
    }

    private fun utf16(body: ByteArray, bigEndian: Boolean): String = CharArray(body.size / 2) {
        val first = body[2 * it].toInt() and BYTE_MASK
        val second = body[2 * it + 1].toInt() and BYTE_MASK
        (if (bigEndian) first shl Byte.SIZE_BITS or second else second shl Byte.SIZE_BITS or first).toChar()
    }.concatToString()

    private fun int32(bytes: ByteArray, at: Int): Int = bigEndian(bytes, at, Byte.SIZE_BITS, BYTE_MASK)

    /** Four bytes of seven bits each, so the size never looks like the MP3 frame sync. */
    private fun syncsafe(bytes: ByteArray, at: Int): Int = bigEndian(bytes, at, SYNCSAFE_BITS, SYNCSAFE_MASK)

    private fun bigEndian(bytes: ByteArray, at: Int, bits: Int, mask: Int): Int =
        (0 until Int.SIZE_BYTES).fold(0) { value, i -> value shl bits or (bytes[at + i].toInt() and mask) }
}
