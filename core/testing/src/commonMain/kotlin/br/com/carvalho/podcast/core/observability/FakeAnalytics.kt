package br.com.carvalho.podcast.core.observability

class FakeAnalytics : Analytics {
    val events = mutableListOf<String>()
    val params = mutableListOf<Map<String, Any?>>()

    override fun logEvent(name: String, params: Map<String, Any?>) {
        events += name
        this.params += params
    }
}
