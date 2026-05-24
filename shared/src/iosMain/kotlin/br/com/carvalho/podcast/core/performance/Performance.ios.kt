package br.com.carvalho.podcast.core.performance

import br.com.carvalho.podcast.core.util.AppLogger
import cocoapods.FirebaseCore.FIRApp
import cocoapods.FirebasePerformance.FIRPerformance
import cocoapods.FirebasePerformance.FIRTrace
import kotlinx.cinterop.ExperimentalForeignApi

private const val TAG = "Performance"

@OptIn(ExperimentalForeignApi::class)
actual object Performance {
    actual fun startTrace(identifier: String): Trace {
        val isFirebaseInitialized = try {
            FIRApp.defaultApp() != null
        } catch (_: Exception) {
            false
        }

        return if (isFirebaseInitialized) {
            try {
                val firTrace = FIRPerformance.sharedInstance().traceWithName(identifier)
                firTrace?.start()
                IosTrace(firTrace)
            } catch (e: Exception) {
                AppLogger.d(TAG, "Firebase Performance error: ${e.message}")
                NoOpTrace
            }
        } else {
            AppLogger.d(TAG, "Firebase not initialized, Performance trace skipped: $identifier")
            NoOpTrace
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IosTrace(private val firTrace: FIRTrace?) : Trace {
    override fun stop() {
        firTrace?.stop()
    }

    override fun putAttribute(key: String, value: String) {
        firTrace?.setValue(value, forAttribute = key)
    }

    override fun incrementMetric(name: String, incrementBy: Long) {
        firTrace?.incrementMetric(name, byInt = incrementBy)
    }
}

private object NoOpTrace : Trace {
    override fun stop() {}
    override fun putAttribute(key: String, value: String) {}
    override fun incrementMetric(name: String, incrementBy: Long) {}
}
