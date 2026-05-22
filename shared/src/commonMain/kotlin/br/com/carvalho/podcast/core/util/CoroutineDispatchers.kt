package br.com.carvalho.podcast.core.util

import kotlinx.coroutines.CoroutineDispatcher
import io.ktor.utils.io.ioDispatcher

data class CoroutineDispatchers(
    val main: CoroutineDispatcher,
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher = ioDispatcher()
)
