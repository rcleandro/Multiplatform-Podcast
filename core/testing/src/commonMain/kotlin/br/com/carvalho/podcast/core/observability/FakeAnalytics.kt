package br.com.carvalho.podcast.core.observability

class FakeAnalytics : Analytics {
    val events = mutableListOf<String>()

    override fun logEvent(name: String, params: Map<String, Any?>) {
        events += name
    }
}
