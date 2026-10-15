package br.com.carvalho.podcast.data.remote

private const val SYNCSAFE_BITS = 7
private const val SYNCSAFE_MASK = 0x7F
private const val BYTE_MASK = 0xFF

/** Builds the ID3 tags the chapter tests read: a header, the frames, and optional padding. */
internal fun id3Tag(version: Int, vararg frames: ByteArray, padding: Int = 0, extendedHeader: Boolean = false): ByteArray {
    val extended = when {
        !extendedHeader -> ByteArray(0)
        version == 4 -> syncsafe(6) + byteArrayOf(1, 0)
        else -> int32(6) + ByteArray(6)
    }
    val body = frames.fold(extended) { all, frame -> all + frame } + ByteArray(padding)
    val flags: Byte = if (extendedHeader) 0x40 else 0
    return "ID3".encodeToByteArray() + byteArrayOf(version.toByte(), 0, flags) + syncsafe(body.size) + body
}

internal fun frame(version: Int, id: String, body: ByteArray): ByteArray =
    id.encodeToByteArray() + (if (version == 4) syncsafe(body.size) else int32(body.size)) + byteArrayOf(0, 0) + body

internal fun chapterFrame(version: Int, element: String, startMs: Int, vararg frames: ByteArray): ByteArray =
    frame(
        version,
        "CHAP",
        element.encodeToByteArray() + 0 + int32(startMs) + int32(0) + int32(-1) + int32(-1) +
            frames.fold(ByteArray(0)) { all, frame -> all + frame },
    )

/** `TIT2` in UTF-8 (encoding 3), null-terminated as many tools write it. */
internal fun title(version: Int, text: String): ByteArray =
    frame(version, "TIT2", byteArrayOf(3) + text.encodeToByteArray() + 0)

/** `TIT2` in UTF-16 with a little-endian byte order mark (encoding 1), as older taggers write it. */
internal fun utf16Title(version: Int, text: String): ByteArray = frame(
    version,
    "TIT2",
    byteArrayOf(1, 0xFF.toByte(), 0xFE.toByte()) +
        text.flatMap { listOf((it.code and BYTE_MASK).toByte(), (it.code shr 8).toByte()) }.toByteArray(),
)

internal fun latin1Title(version: Int, text: String): ByteArray =
    frame(version, "TIT2", byteArrayOf(0) + text.map { it.code.toByte() }.toByteArray())

private operator fun ByteArray.plus(byte: Int): ByteArray = this + byte.toByte()

private fun int32(value: Int) = ByteArray(4) { (value shr (8 * (3 - it))).toByte() }

private fun syncsafe(value: Int) = ByteArray(4) { (value shr (SYNCSAFE_BITS * (3 - it)) and SYNCSAFE_MASK).toByte() }
