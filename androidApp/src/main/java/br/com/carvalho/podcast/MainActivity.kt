package br.com.carvalho.podcast

import android.Manifest
import android.content.Intent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import br.com.carvalho.podcast.core.designsystem.LocalTabletopFold
import br.com.carvalho.podcast.core.designsystem.TabletopFold
import br.com.carvalho.podcast.core.designsystem.tabletopFold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.AppLanguage
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.presentation.isDark
import br.com.carvalho.podcast.presentation.navigation.RootComponent
import org.koin.android.ext.android.inject
import com.arkivanov.decompose.defaultComponentContext

private const val TAG = "MainActivity"

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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        root = RootComponent(defaultComponentContext())
        // Only on a fresh start: a recreated activity already took its link.
        if (savedInstanceState == null) openPodcastLink(intent)

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
            CompositionLocalProvider(LocalTabletopFold provides fold) {
                App(root)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openPodcastLink(intent)
    }

    /** A podcast link the app was opened with (18.4) goes to "Add podcast". */
    private fun openPodcastLink(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) intent.dataString?.let(root::onPodcastLink)
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
