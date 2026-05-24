package br.com.carvalho.podcast.core.performance

import cocoapods.FirebaseCore.FIRApp
import cocoapods.FirebasePerformance.FIRPerformance
import cocoapods.FirebasePerformance.FIRTrace
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual object Performance {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
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
                IosTraceImpl(firTrace)
            } catch (_: Exception) {
                NoOpTrace
            }
        } else {
            NoOpTrace
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IosTraceImpl(private val firTrace: FIRTrace?) : Trace {
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
