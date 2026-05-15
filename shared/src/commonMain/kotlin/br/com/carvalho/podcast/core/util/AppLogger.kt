package br.com.carvalho.podcast.core.util

expect object AppLogger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
