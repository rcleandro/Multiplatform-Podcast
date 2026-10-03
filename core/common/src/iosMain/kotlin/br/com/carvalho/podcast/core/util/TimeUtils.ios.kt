package br.com.carvalho.podcast.core.util

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun getCurrentTimestamp(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
