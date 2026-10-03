package br.com.carvalho.podcast.core.extensions

fun Long.toDuration(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}min"
}

fun Long.toTime(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}
