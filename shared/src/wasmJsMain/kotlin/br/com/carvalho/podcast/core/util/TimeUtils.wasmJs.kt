package br.com.carvalho.podcast.core.util

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual fun getCurrentTimestamp(): Long = jsDateNow().toLong()
