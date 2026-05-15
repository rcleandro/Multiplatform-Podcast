package br.com.carvalho.podcast.core.util

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object AppContext {
    private var _context: Any? = null

    var context: Any
        get() = _context ?: throw IllegalStateException("Context not initialized")
        set(value) {
            _context = value
        }
}
