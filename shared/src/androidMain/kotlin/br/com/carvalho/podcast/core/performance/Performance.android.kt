package br.com.carvalho.podcast.core.performance

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace as FirebaseTrace

actual object Performance {
    actual fun startTrace(identifier: String): Trace {
        val firebaseTrace = FirebasePerformance.getInstance().newTrace(identifier)
        firebaseTrace.start()
        return AndroidTrace(firebaseTrace)
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
