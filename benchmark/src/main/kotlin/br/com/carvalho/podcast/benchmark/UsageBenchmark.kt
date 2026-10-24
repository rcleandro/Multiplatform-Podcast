package br.com.carvalho.podcast.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The budgets of 23.1 past the cold start: frames while scrolling the Episodes tab, frames while the player opens, and
 * the memory the app holds after scrolling. Run on the user's own library, with the app in Portuguese and an episode
 * already in the mini player; the texts below are the Portuguese ones. Budgets and results: docs/metricas.md.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class UsageBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun scrollTheEpisodes() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric(), MemoryUsageMetric(MemoryUsageMetric.Mode.Max)),
        compilationMode = CompilationMode.Partial(),
        iterations = ITERATIONS,
        setupBlock = {
            startOnTheLibrary()
            device.findObject(By.text(EPISODES_TAB)).click()
            device.wait(Until.hasObject(By.scrollable(true)), TIMEOUT_MS)
        },
    ) {
        val list = device.findObjects(By.scrollable(true)).maxBy { it.visibleBounds.height() }
        // Keeps the fling clear of the system's gesture area at the bottom edge.
        list.setGestureMargin(device.displayHeight / GESTURE_MARGIN_FRACTION)
        repeat(FLINGS) { list.fling(Direction.DOWN) }
        device.waitForIdle()
    }

    @Test
    fun openThePlayer() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = ITERATIONS,
        setupBlock = {
            startOnTheLibrary()
            // The mini player shows once the last episode is read back from the database, after the first frame.
            device.wait(Until.hasObject(By.desc(OPEN_QUEUE)), TIMEOUT_MS)
        },
    ) {
        // The mini player's title sits left of its queue button; a tap there opens the player.
        val queue = device.findObject(By.desc(OPEN_QUEUE)).visibleBounds
        device.click(queue.left - queue.width() * TITLE_OFFSET, queue.centerY())
        device.wait(Until.gone(By.desc(OPEN_QUEUE)), TIMEOUT_MS)
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.startOnTheLibrary() {
        // Outside a startup benchmark the app survives between iterations, and the last one left the player open.
        killProcess()
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text(EPISODES_TAB)), TIMEOUT_MS)
    }

    private companion object {
        const val PACKAGE = "br.com.carvalho.podcast"
        const val EPISODES_TAB = "Episódios"
        const val OPEN_QUEUE = "Abrir fila"
        const val ITERATIONS = 5
        const val FLINGS = 5
        const val TITLE_OFFSET = 3
        const val GESTURE_MARGIN_FRACTION = 5
        const val TIMEOUT_MS = 10_000L
    }
}
