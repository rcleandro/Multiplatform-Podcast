package br.com.carvalho.podcast

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import br.com.carvalho.podcast.core.designsystem.LocalPictureInPicture
import br.com.carvalho.podcast.core.designsystem.LocalTabletopFold
import br.com.carvalho.podcast.core.designsystem.TabletopFold
import br.com.carvalho.podcast.core.designsystem.tabletopFold
import br.com.carvalho.podcast.core.safety.PlayAgeSignalSource
import br.com.carvalho.podcast.domain.model.AgeGroup
import br.com.carvalho.podcast.domain.repository.AgeSignalSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.AppLanguage
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.shortcut.AppShortcuts
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.ChildSafety
import br.com.carvalho.podcast.presentation.isDark
import br.com.carvalho.podcast.presentation.navigation.RootComponent
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import com.arkivanov.decompose.defaultComponentContext

private const val TAG = "MainActivity"
private val RESTORE_TIMEOUT = 5.seconds
private const val DEBUG_COUNTRY = "debug_country"
private const val DEBUG_AGE_GROUP = "debug_age_group"

class MainActivity : AppCompatActivity() {

    private lateinit var root: RootComponent

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            AppLogger.d(TAG, "Notification permission granted")
        } else {
            AppLogger.d(TAG, "Notification permission denied")
        }
    }

    private val childSafety: ChildSafety by inject()

    /**
     * Debug builds only: `debug_country` and `debug_age_group` extras (Maestro's launch arguments) stand in for the
     * store, so the ECA Digital flows run without a minor's account (26.9). A release build ignores them.
     */
    private fun overrideAgeSignalForTests() {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val country = intent.getStringExtra(DEBUG_COUNTRY) ?: return
        val ageGroup = intent.getStringExtra(DEBUG_AGE_GROUP)?.let { AgeGroup.valueOf(it) } ?: AgeGroup.UNKNOWN
        (get<AgeSignalSource>() as? PlayAgeSignalSource)?.debugOverride = country to ageGroup
    }

    // Brazil's age signal (26.2): read on opening and each time the app comes back to the front.
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { childSafety.refresh() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        overrideAgeSignalForTests()

        requestNotificationPermission()

        root = RootComponent(defaultComponentContext())
        // Only on a fresh start: a recreated activity already took its link.
        if (savedInstanceState == null) openIntent(intent)
        // Here and not in the Application: the activity starts again when the language changes.
        lifecycleScope.launch { AppShortcuts.publish(this@MainActivity) }

        // A half open foldable lays the player out around its fold (21.3).
        val tabletopFolds = WindowInfoTracker.getOrCreate(this).windowLayoutInfo(this).map { it.tabletopFold() }

        val preferences: PreferencesRepository by inject()
        followAppLanguage(preferences)

        setContent {
            // The status and navigation bar icons follow the app's theme, which may differ from the system's (18.5).
            val dark = preferences.themeMode.collectAsState().value.isDark()
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            val fold by tabletopFolds.collectAsState(initial = null)
            // A video playing when the user leaves the app shrinks to picture-in-picture (19.10).
            val pictureInPicture by produceState(isInPictureInPictureMode) {
                val listener = Consumer<PictureInPictureModeChangedInfo> { value = it.isInPictureInPictureMode }
                addOnPictureInPictureModeChangedListener(listener)
                awaitDispose { removeOnPictureInPictureModeChangedListener(listener) }
            }
            CompositionLocalProvider(LocalTabletopFold provides fold, LocalPictureInPicture provides pictureInPicture) {
                App(root)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openIntent(intent)
    }

    /** A podcast link (18.4) goes to "Add podcast"; a launcher shortcut (19.7) opens what it names. */
    private fun openIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) intent.dataString?.let(root::onPodcastLink)
        if (AppShortcuts.open(intent?.action, root)) resumeOnceRestored()
    }

    /** On a cold start the player is still loading the last episode: play it once it is there. */
    private fun resumeOnceRestored() {
        val player: AudioPlayer by inject()
        lifecycleScope.launch {
            withTimeoutOrNull(RESTORE_TIMEOUT) { player.playerState.first { it.currentEpisode != null } }
                ?.let { player.resume() }
        }
    }

    /**
     * The whole app, notifications included, speaks the language chosen in Settings (18.5). Android keeps it as the
     * app's own language, which can also change in the system settings: that one wins when the app opens.
     */
    private fun followAppLanguage(preferences: PreferencesRepository) {
        fun current() = AppLanguage.of(AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag())
        preferences.setAppLanguage(current())
        lifecycleScope.launch {
            preferences.appLanguage.collect { language ->
                if (language != current()) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag.orEmpty()))
                }
            }
        }
    }

    private fun WindowLayoutInfo.tabletopFold(): TabletopFold? = displayFeatures
        .filterIsInstance<FoldingFeature>()
        .firstNotNullOfOrNull {
            tabletopFold(
                isHalfOpened = it.state == FoldingFeature.State.HALF_OPENED,
                isAcross = it.orientation == FoldingFeature.Orientation.HORIZONTAL,
                top = it.bounds.top,
                bottom = it.bounds.bottom,
            )
        }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
