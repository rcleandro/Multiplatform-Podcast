package br.com.carvalho.podcast.core.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.provider.Settings

/**
 * The watch's media volume (19.9e), changed the system's way so the watch shows its own volume indicator: the crown
 * or bezel steps it, and the volume button only shows the indicator, to be turned from there.
 */
class WatchVolume(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)

    fun step(up: Boolean) = adjust(if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER)

    fun show() = adjust(AudioManager.ADJUST_SAME)

    private fun adjust(direction: Int) =
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
}

private const val OUTPUT_SWITCHER = "com.android.settings.panel.action.MEDIA_OUTPUT"
private const val OUTPUT_SWITCHER_PACKAGE = "com.android.settings.panel.extra.PACKAGE_NAME"
private const val CONNECTION_ONLY = "EXTRA_CONNECTION_ONLY"
private const val CLOSE_ON_CONNECT = "EXTRA_CLOSE_ON_CONNECT"
private const val FILTER_TYPE = "android.bluetooth.devicepicker.extra.FILTER_TYPE"
private const val FILTER_TYPE_AUDIO = 1

/**
 * Where the episode is heard (19.9e): the system's output switcher from Wear OS 5, which lists the watch speaker and
 * the paired headphones; before it, the Bluetooth settings showing only audio devices. As Horologist does it.
 */
fun Context.chooseAudioOutput() {
    val switcher = Intent(OUTPUT_SWITCHER)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(OUTPUT_SWITCHER_PACKAGE, packageName)
    // Only the system's own switcher: any app could declare the action.
    val systemFlags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
    val system = packageManager.queryIntentActivities(switcher, 0).firstOrNull {
        it.activityInfo.applicationInfo.flags and systemFlags != 0
    }
    if (system != null) {
        startActivity(switcher.setComponent(ComponentName(system.activityInfo.packageName, system.activityInfo.name)))
        return
    }
    startActivity(
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(CONNECTION_ONLY, true)
            .putExtra(CLOSE_ON_CONNECT, true)
            .putExtra(FILTER_TYPE, FILTER_TYPE_AUDIO)
    )
}

/** Where a watch's sound may go when only headphones will do (19.9f): Bluetooth, USB and wired headsets, speakers. */
private val HEADPHONE_TYPES = setOf(
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLE_SPEAKER,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_HEARING_AID,
)

/** Whether headphones or a speaker other than the watch's own is connected. */
fun Context.hasHeadphones(): Boolean = getSystemService(AudioManager::class.java)
    .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    .any { it.type in HEADPHONE_TYPES }
