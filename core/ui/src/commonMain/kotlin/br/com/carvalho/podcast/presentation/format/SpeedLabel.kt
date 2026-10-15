package br.com.carvalho.podcast.presentation.format

/** "1×", "1.5×", "0.8×": no trailing ".0". */
fun formatSpeed(speed: Float): String = speed.toString().removeSuffix(".0") + "×"
