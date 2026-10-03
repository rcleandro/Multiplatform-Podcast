package br.com.carvalho.podcast.core.designsystem

import coil3.request.ImageRequest
import coil3.request.allowHardware

internal actual fun ImageRequest.Builder.softwareBitmap(): ImageRequest.Builder = allowHardware(false)
