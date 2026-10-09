package br.com.carvalho.podcast.presentation.share

import androidx.compose.runtime.Composable
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

@Composable
internal actual fun rememberShareSheet(): ((String) -> Unit)? = ::presentShareSheet

private fun presentShareSheet(text: String) {
    @Suppress("DEPRECATION") // keyWindow: the app has a single window
    var top: UIViewController? = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    // An iPad shows the sheet as a popover, which needs somewhere to point.
    sheet.popoverPresentationController?.sourceView = top?.view
    top?.presentViewController(sheet, animated = true, completion = null)
}
