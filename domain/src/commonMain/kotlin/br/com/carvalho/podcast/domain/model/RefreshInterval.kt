package br.com.carvalho.podcast.domain.model

/** How often the app looks for new episodes while closed (18.18): the hours offered, 0 for never, and the default. */
object RefreshInterval {
    @Suppress("MagicNumber") // the choices themselves
    val CHOICES = listOf(1, 3, 6, 12, 24, 0)
    const val DEFAULT_HOURS = 6
}
