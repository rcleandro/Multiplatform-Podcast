package br.com.carvalho.podcast.core.util

import okio.FileSystem
import okio.Path

/**
 * Utilitário expect para obter o sistema de arquivos e diretórios base para cada plataforma.
 */
expect object FileUtils {
    /**
     * Retorna o FileSystem da plataforma (Okio).
     */
    val fileSystem: FileSystem

    /**
     * Retorna o diretório base para salvar arquivos de dados (ex: downloads).
     */
    val baseDir: Path
}
