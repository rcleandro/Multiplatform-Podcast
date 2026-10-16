package br.com.carvalho.podcast.wear

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.AudioPlayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.seconds

/**
 * The episode playing on a watch face (19.9f): the time left in a short slot, the title over the podcast in a long one,
 * the progress as an arc with the time left.
 * A tap opens the player; with nothing loaded the slot stays empty.
 */
class NowPlayingComplicationService : SuspendingComplicationDataSourceService() {
    private val player: AudioPlayer by inject()

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData =
        data(request.complicationType, player.restoredState())

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        data(type, PlayerState(position = PREVIEW_POSITION_MS, duration = PREVIEW_LENGTH_MS), preview = true)

    private fun data(type: ComplicationType, state: PlayerState, preview: Boolean = false): ComplicationData {
        val episode = state.currentEpisode
        val left = timeLeft(state)
        // A known time left means a known length.
        if (left == null || (episode == null && !preview)) return NoDataComplicationData()
        val title = episode?.title ?: getString(R.string.app_name)
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_launcher_foreground)).build()
        val description = plain(title)
        return when (type) {
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(plain(title), description)
                .setTitle(plain(episode?.podcastTitle ?: left))
                .setMonochromaticImage(icon)
                .setTapAction(openApp())
                .build()
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(plain(left), description)
                .setMonochromaticImage(icon)
                .setTapAction(openApp())
                .build()
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = state.position.toFloat(),
                min = 0f,
                max = lengthMs(state)?.toFloat() ?: 0f,
                contentDescription = description,
            )
                .setText(plain(left))
                .setMonochromaticImage(icon)
                .setTapAction(openApp())
                .build()
            else -> NoDataComplicationData()
        }
    }

    private fun plain(text: String) = PlainComplicationText.Builder(text).build()

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, WearActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(WearActivity.EXTRA_OPEN_PLAYER, true),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        /** The face editor's preview: a third heard of half an hour. */
        const val PREVIEW_POSITION_MS = 600_000L
        const val PREVIEW_LENGTH_MS = 1_800_000L
    }
}

private const val MILLIS_PER_SECOND = 1000
private val RESTORE_WAIT = 2.seconds
private const val SECONDS_PER_MINUTE = 60L

/**
 * What is left of the episode playing, as "20min", never under a minute. Until the player has loaded the audio, as
 * after a restart, the feed's length stands in; null when neither is known.
 */
internal fun timeLeft(state: PlayerState): String? {
    val duration = lengthMs(state) ?: return null
    val seconds = (duration - state.position) / MILLIS_PER_SECOND
    return seconds.coerceAtLeast(SECONDS_PER_MINUTE).toDuration()
}

/** The episode's length in milliseconds: the player's once it has the audio, else the feed's. */
internal fun lengthMs(state: PlayerState): Long? =
    state.duration?.takeIf { it > 0 } ?: state.currentEpisode?.duration?.takeIf { it > 0 }?.times(MILLIS_PER_SECOND)

/**
 * The player's state once it has brought the last session back: a tile or the face can wake the process, and ask
 * before that has happened. With nothing to bring back, the state as it is after a short wait.
 */
internal suspend fun AudioPlayer.restoredState(): PlayerState =
    withTimeoutOrNull(RESTORE_WAIT) { playerState.first { it.currentEpisode != null } } ?: playerState.value
