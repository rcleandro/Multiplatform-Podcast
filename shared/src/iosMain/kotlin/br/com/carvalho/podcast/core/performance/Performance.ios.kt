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
        } catch (e: Exception) {
            false
        }

        return if (isFirebaseInitialized) {
            try {
                val firTrace = FIRPerformance.sharedInstance().traceWithName(identifier)
                firTrace?.start()
                IosTraceImpl(firTrace)
            } catch (e: Exception) {
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
    override fun stop() {
        // No-op
    }

    override fun putAttribute(key: String, value: String) {
        // No-op
    }

    override fun incrementMetric(name: String, incrementBy: Long) {
        // No-op
    }
}
