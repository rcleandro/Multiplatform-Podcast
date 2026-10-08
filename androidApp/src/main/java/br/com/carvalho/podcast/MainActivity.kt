package br.com.carvalho.podcast

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import br.com.carvalho.podcast.core.designsystem.LocalTabletopFold
import br.com.carvalho.podcast.core.designsystem.TabletopFold
import br.com.carvalho.podcast.core.designsystem.tabletopFold
import kotlinx.coroutines.flow.map
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.presentation.navigation.RootComponent
import com.arkivanov.decompose.defaultComponentContext

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

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

        val root = RootComponent(defaultComponentContext())

        // A half open foldable lays the player out around its fold (21.3).
        val tabletopFolds = WindowInfoTracker.getOrCreate(this).windowLayoutInfo(this).map { it.tabletopFold() }

        setContent {
            val fold by tabletopFolds.collectAsState(initial = null)
            CompositionLocalProvider(LocalTabletopFold provides fold) {
                App(root)
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
