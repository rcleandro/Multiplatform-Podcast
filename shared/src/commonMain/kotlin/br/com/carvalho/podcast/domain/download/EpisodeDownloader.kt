package br.com.carvalho.podcast.domain.download

import br.com.carvalho.podcast.domain.model.Episode
import kotlinx.coroutines.flow.StateFlow

/**
 * Representa o estado de download de um episódio.
 */
sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data class Queued(val priority: Int = 0) : DownloadStatus()
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long?) : DownloadStatus()
    data class Completed(val localPath: String) : DownloadStatus()
    data class Failed(val error: String) : DownloadStatus()
}

/**
 * Interface principal para o gerenciamento de downloads de episódios em todas as plataformas.
 */
interface EpisodeDownloader {
    /**
     * Inicia o download de um episódio.
     */
    suspend fun download(episode: Episode)

    /**
     * Pausa um download em andamento.
     */
    suspend fun pause(episodeId: String)

    /**
     * Retoma um download pausado.
     */
    suspend fun resume(episodeId: String)

    /**
     * Cancela e remove o download de um episódio em andamento.
     */
    suspend fun cancel(episodeId: String)

    /**
     * Exclui o arquivo baixado localmente de um episódio já concluído.
     */
    suspend fun delete(episodeId: String)

    /**
     * Retorna o fluxo de estado de download para um episódio específico.
     */
    fun getDownloadStatus(episodeId: String): StateFlow<DownloadStatus>

    /**
     * Retorna todos os downloads ativos e seus estados.
     */
    val activeDownloads: StateFlow<Map<String, DownloadStatus>>

    /**
     * Verifica se um episódio está disponível localmente e retorna o caminho se sim.
     */
    fun getLocalPath(episodeId: String): String?
}
