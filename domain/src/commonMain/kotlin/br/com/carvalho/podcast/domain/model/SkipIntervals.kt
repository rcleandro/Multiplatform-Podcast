package br.com.carvalho.podcast.domain.model

import br.com.carvalho.podcast.core.AppConfig

/** How far the back and forward buttons jump, in seconds (18.5). */
data class SkipIntervals(
    val backwardSeconds: Int = AppConfig.SKIP_BACKWARD_SECONDS,
    val forwardSeconds: Int = AppConfig.SKIP_FORWARD_SECONDS,
) {
    companion object {
        /**
         * The jumps offered: the ones with a numbered icon both in the app (Material) and in the notification
         * (Media3). Media3 also has 15 s, but the app would show it as a plain arrow beside a numbered one.
         */
        @Suppress("MagicNumber") // the seconds are the choices themselves
        val CHOICES: List<Int> = listOf(5, 10, 30)
    }
}
