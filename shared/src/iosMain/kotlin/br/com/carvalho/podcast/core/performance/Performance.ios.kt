package br.com.carvalho.podcast.core.performance

import cocoapods.FirebasePerformance.FIRPerformance
import cocoapods.FirebasePerformance.FIRTrace
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual object Performance {
    actual fun startTrace(identifier: String): Trace {
        val firTrace = FIRPerformance.sharedInstance().traceWithName(identifier)
        firTrace?.start()
        return IosTrace(firTrace)
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
