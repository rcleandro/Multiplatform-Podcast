package br.com.carvalho.podcast.core.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class CoroutineDispatchers(
    val main: CoroutineDispatcher,
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher = Dispatchers.Default
)
