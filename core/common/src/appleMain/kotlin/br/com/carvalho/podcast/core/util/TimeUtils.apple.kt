package br.com.carvalho.podcast.core.util

import br.com.carvalho.podcast.core.AppConfig
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun getCurrentTimestamp(): Long = (NSDate().timeIntervalSince1970 * AppConfig.MILLIS_PER_SECOND).toLong()
