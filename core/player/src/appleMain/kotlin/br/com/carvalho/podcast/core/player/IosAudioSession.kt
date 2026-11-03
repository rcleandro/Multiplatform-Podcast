package br.com.carvalho.podcast.core.player

/** Sets the app's audio session up once, when the player is made. */
internal expect fun setUpAudioSession()

/**
 * Runs [start] once the session can play. At once on the iPhone; on the Apple Watch (ADR 0016), after the system
 * activated the long-form audio session, which shows its output picker when no headphones are connected.
 */
internal expect fun whenAudioCanPlay(start: () -> Unit)
