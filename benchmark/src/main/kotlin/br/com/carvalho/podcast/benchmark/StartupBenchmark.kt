package br.com.carvalho.podcast.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold start of the app until its first frame, with no compilation and with the code a user would have after a few
 * days (partial). The gap between the two is what the baseline profile of 20.1 can win back. Budget: docs/metricas.md.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartWithoutCompilation() = startup(CompilationMode.None())

    @Test
    fun coldStartPartiallyCompiled() = startup(CompilationMode.Partial())

    private fun startup(compilation: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilation,
        startupMode = StartupMode.COLD,
        iterations = ITERATIONS,
    ) {
        pressHome()
        startActivityAndWait()
    }

    private companion object {
        const val PACKAGE = "br.com.carvalho.podcast"
        const val ITERATIONS = 10
    }
}
