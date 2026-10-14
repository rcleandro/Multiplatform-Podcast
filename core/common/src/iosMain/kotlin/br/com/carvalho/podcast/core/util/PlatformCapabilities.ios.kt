package br.com.carvalho.podcast.core.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual val supportsDownloads: Boolean = true

actual val canLimitDownloadsToWifi: Boolean = true

actual val canEnhanceSpeech: Boolean = false

actual val openSystemLanguageSettings: (() -> Unit)? = {
    NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
        UIApplication.sharedApplication.openURL(it, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}
