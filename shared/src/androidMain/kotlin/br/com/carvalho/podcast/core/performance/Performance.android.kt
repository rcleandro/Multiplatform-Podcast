package br.com.carvalho.podcast.core.performance

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace as FirebaseTrace

actual object Performance {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    actual fun startTrace(identifier: String): Trace {
        return try {
            val firebaseTrace = FirebasePerformance.getInstance().newTrace(identifier)
            firebaseTrace.start()
            AndroidTrace(firebaseTrace)
        } catch (e: Exception) {
            // No unit tests, FirebasePerformance is not available
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
