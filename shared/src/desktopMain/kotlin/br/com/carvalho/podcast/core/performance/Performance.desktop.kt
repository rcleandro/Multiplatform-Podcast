package br.com.carvalho.podcast.core.performance

actual object Performance {
    actual fun startTrace(identifier: String): Trace {
        return NoOpTrace
    }
}

private object DesktopTrace : Trace {
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
