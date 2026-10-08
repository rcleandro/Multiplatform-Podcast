package br.com.carvalho.podcast.core.observability

class FakeAnalytics : Analytics {
    val events = mutableListOf<String>()
    val params = mutableListOf<Map<String, Any?>>()

    override fun logEvent(event: AnalyticsEvent) {
        events += event.name
        params += event.params
    }
}
