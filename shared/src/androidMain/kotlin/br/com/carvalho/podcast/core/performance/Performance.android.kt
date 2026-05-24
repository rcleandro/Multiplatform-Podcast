package br.com.carvalho.podcast.core.performance

import br.com.carvalho.podcast.core.util.AppLogger
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace as FirebaseTrace

private const val TAG = "Performance"

actual object Performance {
    actual fun startTrace(identifier: String): Trace {
        return try {
            val firebaseTrace = FirebasePerformance.getInstance().newTrace(identifier)
            firebaseTrace.start()
            AndroidTrace(firebaseTrace)
        } catch (e: Exception) {
            AppLogger.d(TAG, "Firebase Performance not available: ${e.message}")
            NoOpTrace
        }
    }
}

private class AndroidTrace(private val firebaseTrace: FirebaseTrace) : Trace {
    override fun stop() {
        firebaseTrace.stop()
    }

    override fun putAttribute(key: String, value: String) {
        firebaseTrace.putAttribute(key, value)
    }

    override fun incrementMetric(name: String, incrementBy: Long) {
        firebaseTrace.incrementMetric(name, incrementBy)
    }
}

private object NoOpTrace : Trace {
    override fun stop() {}
    override fun putAttribute(key: String, value: String) {}
    override fun incrementMetric(name: String, incrementBy: Long) {}
}
