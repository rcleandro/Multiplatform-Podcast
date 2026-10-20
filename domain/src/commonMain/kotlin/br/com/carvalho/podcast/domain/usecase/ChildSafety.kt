package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.AgeGroup
import br.com.carvalho.podcast.domain.model.ChildSafetyState
import br.com.carvalho.podcast.domain.repository.AgeSignalSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

private const val BRAZIL = "BR"

/**
 * Who the ECA Digital protects (26.2, ADR 0012), read when the app opens and comes back to the foreground. Until a
 * signal arrives, someone in Brazil counts as unknown: the protective side. Only kept in memory.
 */
class ChildSafety(private val source: AgeSignalSource = AgeSignalSource.None) {
    private val _state = MutableStateFlow(ChildSafetyState(inBrazil = isInBrazil()))
    val state: StateFlow<ChildSafetyState> = _state.asStateFlow()
    private val refreshing = Mutex()

    /** Skipped while one runs: the store's prompt pauses the app, and coming back would ask again. */
    suspend fun refresh() {
        if (!refreshing.tryLock()) return
        try {
            val inBrazil = isInBrazil()
            // Outside Brazil the store is never asked, so no prompt shows there.
            val ageGroup = if (inBrazil) readAgeGroup() else AgeGroup.UNKNOWN
            _state.update { ChildSafetyState(inBrazil, ageGroup ?: it.ageGroup) }
        } finally {
            refreshing.unlock()
        }
    }

    // Any source pointing to Brazil counts: the protective reading.
    private fun isInBrazil() = source.countries().any { it.equals(BRAZIL, ignoreCase = true) }

    /** Null when the store failed: the last answer stays rather than an adult's or a fresh unknown. */
    @Suppress("TooGenericExceptionCaught") // the stores fail in their own ways
    private suspend fun readAgeGroup(): AgeGroup? = try {
        source.ageGroup()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
