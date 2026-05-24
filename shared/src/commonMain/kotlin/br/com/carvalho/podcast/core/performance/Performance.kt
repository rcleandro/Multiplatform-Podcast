package br.com.carvalho.podcast.core.performance

expect object Performance {
    fun startTrace(identifier: String): Trace
}

interface Trace {
    fun stop()
    fun putAttribute(key: String, value: String)
    fun incrementMetric(name: String, incrementBy: Long)
}
