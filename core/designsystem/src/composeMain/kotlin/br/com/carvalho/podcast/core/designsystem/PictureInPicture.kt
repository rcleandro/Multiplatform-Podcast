package br.com.carvalho.podcast.core.designsystem

import androidx.compose.runtime.compositionLocalOf

/** True while the window is shrunk to picture-in-picture (19.10): screens show their video and nothing else. */
val LocalPictureInPicture = compositionLocalOf { false }
