package br.com.carvalho.podcast.core.di

import io.ktor.client.engine.HttpClientEngine
import kotlin.test.Test
import kotlinx.coroutines.CoroutineDispatcher
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/** Fails when a definition needs a type nothing binds, instead of crashing when the screen opens. */
class KoinGraphTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun everyDefinitionCanBeResolved() {
        module { includes(commonModules + platformModule) }.verify(extraTypes = EXTRA_TYPES)
    }

    private companion object {
        /** Built inside definition lambdas from values Koin does not provide. */
        val EXTRA_TYPES = listOf(
            CoroutineDispatcher::class,
            HttpClientEngine::class,
            FileSystem::class,
            Path::class,
        )
    }
}
