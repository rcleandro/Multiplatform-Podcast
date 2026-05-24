package br.com.carvalho.podcast.core.performance

actual object Performance {
    actual fun startTrace(identifier: String): Trace {
        return NoOpTrace
    }
}

private object NoOpTrace : Trace {
    override fun stop() {}
    override fun putAttribute(key: String, value: String) {}
    override fun incrementMetric(name: String, incrementBy: Long) {}
}
