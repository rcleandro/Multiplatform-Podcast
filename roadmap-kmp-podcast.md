# 🎙️ Roadmap — App de Podcast KMP

> **Stack:** Kotlin Multiplatform · Compose Multiplatform · Room 3 · Ktor 3 · Koin 4 · MVVM + Clean Architecture
> **Plataformas:** Android · iOS · Desktop (Windows/macOS/Linux) · Web (WasmJs)
> **Duração estimada total:** ~15–16 semanas
> **Atualizado em:** Maio 2026

---

## ⚠️ Mudanças Críticas de Versão (Room 3)

Room 3 é uma reescrita focada em KMP. As diferenças em relação ao Room 2.x que impactam o projeto são:

| Item | Room 2.x | Room 3.x |
|---|---|---|
| Maven group | `androidx.room` | **`androidx.room3`** |
| Artefatos | `room-runtime` | **`room3-runtime`** |
| Geração de código | Java + Kotlin | **Kotlin only** |
| Processador | KAPT ou KSP | **KSP obrigatório** |
| APIs DAO bloqueantes | Permitidas | **Proibidas — tudo suspend** |
| SupportSQLite | Padrão | **Removido** (use `room3-sqlite-wrapper` só para migração) |
| Suporte Web | Não | **Sim — JS + WasmJs** |
| Driver Web | — | **`WebWorkerSQLiteDriver`** via `sqlite-web` |

---

## 📐 Arquitetura de Referência

```
:androidApp  :iosApp  :desktopApp  :webApp
        ↓        ↓         ↓          ↓
              :shared
     ┌────────────────────────────┐
     │  Presentation (ViewModel)  │  ← StateFlow, UiState, SideEffect
     │  Domain (UseCases/Entities)│  ← Pure Kotlin, zero deps externos
     │  Data (Repo/Sources/DAO)   │  ← Room 3, Ktor 3, RSS Parser
     └────────────────────────────┘
```

---

## 🗂️ Estrutura de Módulos

```
root/
├── shared/
│   └── src/
│       ├── commonMain/
│       │   ├── domain/
│       │   │   ├── model/          # Podcast, Episode, PlayerState
│       │   │   ├── repository/     # Interfaces de repositório
│       │   │   └── usecase/        # Use cases por feature
│       │   ├── data/
│       │   │   ├── local/          # Room 3 DAOs, Entities, DB
│       │   │   ├── remote/         # Ktor 3, RSS Parser
│       │   │   ├── repository/     # Implementações concretas
│       │   │   └── mapper/         # Entity ↔ Domain mappers
│       │   └── presentation/
│       │       └── viewmodel/      # ViewModels compartilhados
│       ├── androidMain/            # expect/actual Android
│       ├── iosMain/                # expect/actual iOS
│       ├── desktopMain/            # expect/actual Desktop
│       └── wasmJsMain/             # expect/actual Web + WebWorker
├── androidApp/
├── iosApp/
├── desktopApp/
└── webApp/
    └── src/
        └── workerMain/             # Web Worker para sqlite-web
```

---

## Fase 1 — Setup do Projeto e Estrutura

> ⏱️ **Estimativa:** 1–2 semanas

### Objetivo

Configurar o projeto KMP com todos os targets, estrutura de pacotes Clean Architecture e todas as dependências nas versões mais recentes.

---

### 1.1 Versões das Dependências — `gradle/libs.versions.toml`

```toml
[versions]
# Kotlin e build
kotlin                  = "2.3.21"
agp                     = "8.11.2"
ksp                     = "2.3.21-2.0.1"
android-compileSdk      = "36"
android-minSdk          = "26"
android-targetSdk       = "36"

# UI
compose-multiplatform   = "1.10.3"
androidx-activity       = "1.13.0"
androidx-lifecycle      = "2.10.0"

# Room 3 — novo grupo androidx.room3 (suporte Web/Wasm)
room3                   = "3.0.0-alpha04"
sqlite                  = "2.5.0-beta01"   # inclui sqlite-web e sqlite-bundled

# Rede e serialização
ktor                    = "3.4.2"
kotlinx-serialization   = "1.10.0"
kotlinx-coroutines      = "1.10.2"

# DI
koin                    = "4.2.0"

# Navegação
decompose               = "3.3.0"

# Imagem assíncrona
kamel                   = "1.0.3"

# Testes
turbine                 = "1.2.0"
mockk                   = "1.14.0"

# Media
javafx                  = "21"

[libraries]
# Room 3 — novo grupo maven
room3-runtime           = { module = "androidx.room3:room3-runtime",  version.ref = "room3" }
room3-compiler          = { module = "androidx.room3:room3-compiler",  version.ref = "room3" }   # ksp only
room3-paging            = { module = "androidx.room3:room3-paging",    version.ref = "room3" }   # opcional

# SQLite drivers
sqlite-bundled          = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
sqlite-web              = { module = "androidx.sqlite:sqlite-web-wasm-js",     version.ref = "sqlite" }   # wasmJs

# Ktor 3
ktor-client-core        = { module = "io.ktor:ktor-client-core",                        version.ref = "ktor" }
ktor-client-android     = { module = "io.ktor:ktor-client-android",                     version.ref = "ktor" }
ktor-client-darwin      = { module = "io.ktor:ktor-client-darwin",                      version.ref = "ktor" }
ktor-client-cio         = { module = "io.ktor:ktor-client-cio",                         version.ref = "ktor" }
ktor-client-js          = { module = "io.ktor:ktor-client-js",                          version.ref = "ktor" }
ktor-content-negotiation= { module = "io.ktor:ktor-client-content-negotiation",         version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json",         version.ref = "ktor" }
ktor-logging            = { module = "io.ktor:ktor-client-logging",                     version.ref = "ktor" }

# Koin 4
koin-core               = { module = "io.insert-koin:koin-core",                 version.ref = "koin" }
koin-android            = { module = "io.insert-koin:koin-android",              version.ref = "koin" }
koin-compose            = { module = "io.insert-koin:koin-compose",              version.ref = "koin" }
koin-compose-viewmodel  = { module = "io.insert-koin:koin-compose-viewmodel",    version.ref = "koin" }

# Serialização e coroutines
kotlinx-serialization   = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core",    version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test",    version.ref = "kotlinx-coroutines" }

# Navegação
decompose               = { module = "com.arkivanov.decompose:decompose",                version.ref = "decompose" }
decompose-compose       = { module = "com.arkivanov.decompose:extensions-compose",       version.ref = "decompose" }

# Imagem
kamel-image             = { module = "media.kamel:kamel-image",                          version.ref = "kamel" }

# Lifecycle
androidx-lifecycle-vm   = { module = "androidx.lifecycle:lifecycle-viewmodel",           version.ref = "androidx-lifecycle" }
androidx-lifecycle-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose",  version.ref = "androidx-lifecycle" }

# Media
javafx-media            = { module = "org.openjfx:javafx-media", version.ref = "javafx" }

# Testes
turbine                 = { module = "app.cash.turbine:turbine",  version.ref = "turbine" }
mockk                   = { module = "io.mockk:mockk",            version.ref = "mockk" }
kotlin-test             = { module = "org.jetbrains.kotlin:kotlin-test" }

[plugins]
kotlin-multiplatform    = { id = "org.jetbrains.kotlin.multiplatform",     version.ref = "kotlin" }
kotlin-serialization    = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-multiplatform   = { id = "org.jetbrains.compose",                  version.ref = "compose-multiplatform" }
compose-compiler        = { id = "org.jetbrains.kotlin.plugin.compose",    version.ref = "kotlin" }
android-application     = { id = "com.android.application",                version.ref = "agp" }
android-library         = { id = "com.android.library",                    version.ref = "agp" }
ksp                     = { id = "com.google.devtools.ksp",                 version.ref = "ksp" }
room                    = { id = "androidx.room",                           version = "3.0.0-alpha03" }
```

---

### 1.2 Configuração do `:shared/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget()
    iosX64(); iosArm64(); iosSimulatorArm64()
    jvm("desktop")
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)

            // Room 3 — novo grupo
            implementation(libs.room3.runtime)
            implementation(libs.sqlite.bundled)        // driver unificado para Android/iOS/Desktop

            // Ktor 3
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.logging)

            // Koin 4
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Kotlinx
            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.coroutines.core)

            // Decompose
            implementation(libs.decompose)
            implementation(libs.decompose.compose)

            // Lifecycle ViewModel (KMP)
            implementation(libs.androidx.lifecycle.vm)

            // Imagem assíncrona
            implementation(libs.kamel.image)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            // Media3 para player
            implementation("androidx.media3:media3-exoplayer:1.5.1")
            implementation("androidx.media3:media3-session:1.5.1")
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(compose.desktop.currentOs)
                // JavaFX Media para player desktop (independente de apps externos)
                val javafxVersion = libs.versions.javafx.get()
                implementation("org.openjfx:javafx-media:$javafxVersion:mac-aarch64") // Exemplo, configurado dinamicamente no build
            }
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            // Driver SQLite para Web (Web Worker + OPFS)
            implementation(libs.sqlite.web)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

// KSP — geração do Room 3 para cada target
dependencies {
    add("kspCommonMainMetadata",  libs.room3.compiler)
    add("kspAndroid",             libs.room3.compiler)
    add("kspIosX64",              libs.room3.compiler)
    add("kspIosArm64",            libs.room3.compiler)
    add("kspIosSimulatorArm64",   libs.room3.compiler)
    add("kspDesktop",             libs.room3.compiler)
    add("kspWasmJs",              libs.room3.compiler)
}

// Room Gradle Plugin — exporta schemas para versionamento
room {
    schemaDirectory("$projectDir/schemas")
}
```

---

### 1.3 Estrutura de Pacotes (por feature)

```
com.seuapp.podcast/
├── feature/
│   ├── library/
│   │   ├── domain/      usecase, model
│   │   ├── data/        repository impl
│   │   └── presentation viewmodel, uistate
│   ├── podcast/
│   ├── episode/
│   ├── player/
│   └── search/
├── core/
│   ├── database/        Room 3 config, migrations
│   ├── network/         Ktor config
│   ├── di/              módulos Koin
│   └── util/            extensions, formatters
```

---

### 1.4 Módulos Koin (DI)

```kotlin
// NetworkModule.kt
val networkModule = module {
    single { createHttpClient(get()) }        // expect fun por plataforma
    single<RssFeedDataSource> { RssFeedDataSourceImpl(get()) }
}

// DatabaseModule.kt
val databaseModule = module {
    single { createAppDatabase() }            // expect fun — driver varia por plataforma
    single { get<AppDatabase>().podcastDao() }
    single { get<AppDatabase>().episodeDao() }
    single { get<AppDatabase>().playbackStateDao() }
}

// RepositoryModule.kt
val repositoryModule = module {
    single<PodcastRepository>  { PodcastRepositoryImpl(get(), get()) }
    single<EpisodeRepository>  { EpisodeRepositoryImpl(get(), get()) }
    single<PlayerRepository>   { PlayerRepositoryImpl(get()) }
}

// ViewModelModule.kt
val viewModelModule = module {
    viewModelOf(::LibraryViewModel)
    viewModelOf(::PodcastDetailViewModel)
    viewModelOf(::PlayerViewModel)
    viewModelOf(::SearchViewModel)
}
```

---

### 1.5 Configuração de CI/CD inicial

- [ ]  Criar `.github/workflows/build.yml` com:
    - Unit tests do `:shared` (`kspCommonMainMetadata` + `testDebugUnitTest`)
    - Build Android (`bundleRelease`)
    - Build Desktop (`packageDistributionForCurrentOS`)
    - Build Web (`wasmJsBrowserDistribution`)
    - Lint + Detekt
- [ ]  Configurar `ktlint` via `jlleitschuh/gradle-ktlint` plugin
- [ ]  Adicionar `detekt` com ruleset customizado
- [ ]  Commits no padrão **Conventional Commits:** `feat:`, `fix:`, `refactor:`, `test:`

---

## Fase 2 — Camada de Dados (Room 3 KMP)

> ⏱️ **Estimativa:** 2 semanas

### Objetivo

Modelar o banco local com **Room 3** (`androidx.room3`). Todas as funções DAO são obrigatoriamente `suspend` ou retornam `Flow`. A API `SupportSQLite` não existe mais — usar apenas `SQLiteDriver`.

---

### 2.1 Domain Models (Pure Kotlin — zero deps)

```kotlin
// Podcast.kt
data class Podcast(
    val id: String,           // feedUrl como PK
    val title: String,
    val description: String,
    val imageUrl: String?,
    val author: String?,
    val language: String?,
    val categories: List<String>,
    val feedUrl: String,
    val siteUrl: String?,
    val lastUpdated: Long,
    val isSubscribed: Boolean,
    val episodeCount: Int = 0
)

// Episode.kt
data class Episode(
    val id: String,
    val podcastId: String,
    val title: String,
    val description: String?,
    val audioUrl: String,
    val imageUrl: String?,
    val duration: Long,          // segundos
    val publishDate: Long,
    val isPlayed: Boolean,
    val playbackPosition: Long,  // milissegundos
    val isDownloaded: Boolean,
    val fileSize: Long?
)

// PlayerState.kt
data class PlayerState(
    val currentEpisode: Episode?,
    val isPlaying: Boolean,
    val position: Long,
    val duration: Long,
    val speed: Float,
    val isBuffering: Boolean,
    val queue: List<Episode>
)
```

---

### 2.2 Entidades Room 3

> **Importante:** importar de `androidx.room3.*`, não de `androidx.room.*`

```kotlin
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val author: String?,
    val language: String?,
    val categories: String,     // JSON serializado com kotlinx.serialization
    val feedUrl: String,
    val siteUrl: String?,
    val lastUpdated: Long,
    val isSubscribed: Boolean
)

@Entity(
    tableName = "episodes",
    foreignKeys = [ForeignKey(
        entity = PodcastEntity::class,
        parentColumns = ["id"],
        childColumns = ["podcastId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("podcastId")]
)
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val podcastId: String,
    val title: String,
    val description: String?,
    val audioUrl: String,
    val imageUrl: String?,
    val duration: Long,
    val publishDate: Long,
    val isPlayed: Boolean,
    val playbackPosition: Long,
    val isDownloaded: Boolean,
    val fileSize: Long?
)

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 1,   // registro singleton
    val episodeId: String?,
    val position: Long,
    val speed: Float,
    val queueJson: String
)
```

---

### 2.3 DAOs (Room 3 — tudo suspend ou Flow)

```kotlin
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Delete
import kotlinx.coroutines.flow.Flow

// PodcastDao.kt
@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcasts WHERE isSubscribed = 1 ORDER BY title ASC")
    fun getAllSubscribed(): Flow<List<PodcastEntity>>       // Flow — reativo, não suspend

    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun getById(id: String): PodcastEntity?        // suspend obrigatório no Room 3

    @Query("SELECT EXISTS(SELECT 1 FROM podcasts WHERE feedUrl = :feedUrl)")
    suspend fun existsByFeedUrl(feedUrl: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(podcast: PodcastEntity)

    @Delete
    suspend fun delete(podcast: PodcastEntity)

    @Query("UPDATE podcasts SET lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateLastUpdated(id: String, timestamp: Long)
}

// EpisodeDao.kt
@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishDate DESC")
    fun getByPodcast(podcastId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE isPlayed = 0 ORDER BY publishDate DESC")
    fun getUnplayed(): Flow<List<EpisodeEntity>>

    @Query("""
        SELECT * FROM episodes
        WHERE title LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        ORDER BY publishDate DESC
    """)
    fun search(query: String): Flow<List<EpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM episodes WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Query("""
        UPDATE episodes
        SET isPlayed = :played, playbackPosition = :position
        WHERE id = :id
    """)
    suspend fun updatePlayback(id: String, played: Boolean, position: Long)

    @Query("SELECT COUNT(*) FROM episodes WHERE podcastId = :podcastId AND isPlayed = 0")
    fun getUnplayedCount(podcastId: String): Flow<Int>
}

// PlaybackStateDao.kt
@Dao
interface PlaybackStateDao {
    @Query("SELECT * FROM playback_state WHERE id = 1")
    suspend fun get(): PlaybackStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(state: PlaybackStateEntity)
}
```

---

### 2.4 Banco de Dados e Drivers por Plataforma

#### Declaração (commonMain)

```kotlin
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class, PlaybackStateEntity::class],
    version = 1
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun playbackStateDao(): PlaybackStateDao
}

// O compilador Room 3 gera os actual automaticamente via KSP
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

// Função de criação — expect/actual por plataforma
expect fun createAppDatabase(): AppDatabase
```

#### Android (androidMain)

```kotlin
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual fun createAppDatabase(): AppDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.databaseBuilder<AppDatabase>(context, "podcast.db")
        .setDriver(BundledSQLiteDriver())          // consistência cross-platform
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
```

#### iOS (iosMain)

```kotlin
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSHomeDirectory

actual fun createAppDatabase(): AppDatabase {
    val dbPath = NSHomeDirectory() + "/Documents/podcast.db"
    return Room.databaseBuilder<AppDatabase>(dbPath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
```

#### Desktop (desktopMain)

```kotlin
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual fun createAppDatabase(): AppDatabase {
    val dbFile = File(System.getProperty("user.home"), ".podcast/podcast.db")
        .also { it.parentFile?.mkdirs() }
    return Room.databaseBuilder<AppDatabase>(dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
```

#### Web — wasmJs (wasmJsMain)

> Room 3 + `sqlite-web` usa **Web Worker** + **OPFS** (Origin Private File System) para persistência.

```kotlin
import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

actual fun createAppDatabase(): AppDatabase {
    return Room.databaseBuilder<AppDatabase>("podcast.db")
        .setDriver(WebWorkerSQLiteDriver(createSqliteWorker()))
        .build()
    // Nota: sem setQueryCoroutineContext — a Web é assíncrona por natureza
}

// Worker que executa o SQLite WASM em background thread
private fun createSqliteWorker(): Worker =
    Worker(js("""new URL("sqlite-web-worker/worker.js", import.meta.url)"""))
```

**Configuração adicional necessária no webApp:**

```kotlin
// webApp/build.gradle.kts — adicionar dependência NPM do worker
kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                // Web Worker precisa ser copiado para o output
                devServer = KotlinWebpackConfig.DevServer(
                    open = false,
                    port = 8080
                )
            }
        }
    }
}

// O worker NPM é publicado como pacote local — criar módulo "sqlite-web-worker"
// Consultar: https://github.com/androidx/androidx/tree/main/room/room-samples
```

---

### 2.5 Mappers Entity ↔ Domain

```kotlin
// PodcastMapper.kt
fun PodcastEntity.toDomain(): Podcast = Podcast(
    id = id, title = title, description = description,
    imageUrl = imageUrl, author = author, language = language,
    categories = Json.decodeFromString(categories),
    feedUrl = feedUrl, siteUrl = siteUrl,
    lastUpdated = lastUpdated, isSubscribed = isSubscribed
)

fun Podcast.toEntity(): PodcastEntity = PodcastEntity(
    id = id, title = title, description = description,
    imageUrl = imageUrl, author = author, language = language,
    categories = Json.encodeToString(categories),
    feedUrl = feedUrl, siteUrl = siteUrl,
    lastUpdated = lastUpdated, isSubscribed = isSubscribed
)

// EpisodeMapper.kt — mesmo padrão
fun EpisodeEntity.toDomain(): Episode = Episode(...)
fun Episode.toEntity(podcastId: String): EpisodeEntity = EpisodeEntity(...)
```

---

## Fase 3 — Parsing de Feed RSS

> ⏱️ **Estimativa:** 2 semanas

### Objetivo

Implementar busca e parsing de feeds RSS 2.0/Atom com suporte ao namespace `itunes:`, usando Ktor 3 e parser XML multiplatform.

---

### 3.1 Configuração do Ktor 3 Client

```kotlin
// HttpClientFactory.kt (commonMain)
expect fun createHttpClient(): HttpClient

// androidMain
actual fun createHttpClient(): HttpClient = HttpClient(Android) {
    install(ContentNegotiation) { }
    install(HttpTimeout) {
        requestTimeoutMillis  = 30_000
        connectTimeoutMillis  = 15_000
        socketTimeoutMillis   = 30_000
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 3)
        exponentialDelay()
    }
    install(Logging) {
        logger = Logger.DEFAULT
        level  = LogLevel.INFO
    }
}

// iosMain
actual fun createHttpClient(): HttpClient = HttpClient(Darwin) { /* mesmos plugins */ }

// desktopMain
actual fun createHttpClient(): HttpClient = HttpClient(CIO) { /* mesmos plugins */ }

// wasmJsMain
actual fun createHttpClient(): HttpClient = HttpClient(Js) { /* mesmos plugins */ }
```

---

### 3.2 RSS Parser e Data Source

```kotlin
// RssFeed.kt — modelo intermediário de parsing (commonMain)
data class RssFeed(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val author: String?,
    val language: String?,
    val categories: List<String>,
    val link: String?,
    val ttl: Int?,               // cache hint do servidor
    val episodes: List<RssEpisode>
)

data class RssEpisode(
    val guid: String,
    val title: String,
    val description: String?,
    val enclosureUrl: String,    // URL do arquivo de áudio
    val enclosureType: String?,  // "audio/mpeg", "audio/mp4"
    val duration: String?,       // "HH:MM:SS" ou segundos como string
    val publishDate: String,     // RFC 2822
    val imageUrl: String?,       // itunes:image
    val explicit: Boolean,
    val season: Int?,
    val episode: Int?
)

// RssFeedDataSource.kt
interface RssFeedDataSource {
    suspend fun fetchFeed(url: String): Result<RssFeed>
    suspend fun validateFeedUrl(url: String): Result<Boolean>
}

// RssFeedDataSourceImpl.kt
class RssFeedDataSourceImpl(private val client: HttpClient) : RssFeedDataSource {

    override suspend fun fetchFeed(url: String): Result<RssFeed> = runCatching {
        val response = client.get(url) {
            headers {
                // ETag e Last-Modified para evitar download repetido
                cachedETag[url]?.let { append(HttpHeaders.IfNoneMatch, it) }
                cachedLastModified[url]?.let { append(HttpHeaders.IfModifiedSince, it) }
            }
        }
        if (response.status == HttpStatusCode.NotModified) {
            return Result.success(cachedFeeds[url]!!)
        }
        // Salvar ETag e Last-Modified para próxima requisição
        response.headers[HttpHeaders.ETag]?.let { cachedETag[url] = it }
        response.headers[HttpHeaders.LastModified]?.let { cachedLastModified[url] = it }

        val xmlContent = response.bodyAsText()
        RssXmlParser.parse(xmlContent)
    }

    override suspend fun validateFeedUrl(url: String): Result<Boolean> = runCatching {
        val response = client.head(url)
        val contentType = response.contentType()?.toString() ?: ""
        contentType.contains("xml") || contentType.contains("rss")
    }
}
```

**Campos do namespace `itunes:` a suportar:**

```
itunes:image href="..."       → Episode.imageUrl / Podcast.imageUrl
itunes:duration               → Episode.duration (HH:MM:SS ou segundos)
itunes:author                 → Podcast.author / Episode.author
itunes:category text="..."    → Podcast.categories
itunes:explicit               → Episode.explicit ("true", "yes", "false", "no")
itunes:episode                → Episode.episode (número do episódio)
itunes:season                 → Episode.season
itunes:title                  → fallback para <title>
itunes:summary                → fallback para <description>
```

**Biblioteca de parsing XML recomendada:**

```toml
# libs.versions.toml
xml-util = "0.90.3"

[libraries]
xml-serialization = { module = "io.github.pdvrieze.xmlutil:serialization", version.ref = "xml-util" }
```

---

### 3.3 Tratamento de Erros e Sealed Class

```kotlin
// PodcastError.kt (domain)
sealed class PodcastError : Exception() {
    data object InvalidUrl        : PodcastError()
    data object NetworkError      : PodcastError()
    data object InvalidFeed       : PodcastError()
    data class  AlreadyExists(val podcast: Podcast) : PodcastError()
    data class  ParseError(val cause: String)       : PodcastError()
    data object NotFound          : PodcastError()
    data object NoInternet        : PodcastError()
}
```

---

### 3.4 Use Cases

```kotlin
// AddPodcastFromUrlUseCase.kt
class AddPodcastFromUrlUseCase(
    private val rssDataSource: RssFeedDataSource,
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository
) {
    suspend operator fun invoke(url: String): Result<Podcast> {
        if (!isValidHttpUrl(url)) return Result.failure(PodcastError.InvalidUrl)

        val existingCheck = podcastRepository.getByFeedUrl(url)
        if (existingCheck != null)
            return Result.failure(PodcastError.AlreadyExists(existingCheck))

        return rssDataSource.fetchFeed(url)
            .mapCatching { feed ->
                val podcast = feed.toPodcast(feedUrl = url)
                val episodes = feed.episodes.map { it.toEpisode(podcastId = podcast.id) }
                podcastRepository.save(podcast)
                episodeRepository.insertAll(episodes)
                podcast
            }
    }
}

// RefreshAllPodcastsUseCase.kt
// RefreshPodcastUseCase.kt
// GetLibraryUseCase.kt       → Flow<List<Podcast>>
// GetEpisodesByPodcastUseCase.kt
// SearchEpisodesUseCase.kt
// ImportOpmlUseCase.kt
// ExportOpmlUseCase.kt
// DeletePodcastUseCase.kt
// MarkEpisodePlayedUseCase.kt
```

---

## Fase 4 — Player de Áudio Multiplataforma

> ⏱️ **Estimativa:** 3 semanas

### Objetivo

Implementar player de áudio com arquitetura `expect/actual`, persistindo estado no Room 3 e expondo um `PlayerViewModel` compartilhado em `commonMain`.

---

### 4.1 Interface Compartilhada (commonMain)

```kotlin
// AudioPlayer.kt
expect class AudioPlayer {
    val playerState: StateFlow<PlayerState>
    suspend fun play(episode: Episode)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun skipForward(seconds: Int = 30)
    fun skipBackward(seconds: Int = 15)
    fun release()
}
```

---

### 4.2 Implementação por Plataforma

**Android (androidMain) — AndroidX Media3 1.5.1**

- [ ]  `actual class AudioPlayer` encapsula `ExoPlayer` da Media3
- [ ]  `MediaSessionService` como `ForegroundService` para playback em background
- [ ]  `MediaSession` + `MediaLibrarySession` para Android Auto
- [ ]  `DefaultMediaNotificationProvider` para notificação com controles
- [ ]  `AudioFocusRequest` para ducking de áudio em chamadas
- [ ]  Gerenciar `lifecycle` com `ProcessLifecycleOwner`

**iOS (iosMain) — AVFoundation**

- [ ]  `actual class AudioPlayer` usa `AVPlayer` via Kotlin/Native interop
- [ ]  `AVAudioSession.sharedInstance().setCategory(.playback)` em `iosMain`
- [ ]  `MPNowPlayingInfoCenter` com artwork, título, progresso
- [ ]  `MPRemoteCommandCenter` para comandos da Central de Controles e lock screen
- [ ]  `UIBackgroundModes: [audio]` no `Info.plist`

**Desktop (desktopMain) — JavaFX Media**

- [x]  `actual class AudioPlayer` usa `javafx.scene.media.MediaPlayer`
- [x]  Independente de instalação externa (VLC removido)
- [x]  Sincronização forçada de velocidade para evitar resets do motor nativo
- [ ]  MPRIS2 no Linux via `DBus` para controles do sistema (KDE/GNOME)
- [ ]  Media keys no macOS via `NSEvent` global monitor

**Web (wasmJsMain) — HTML5 Audio API**

- [x]  `actual class AudioPlayer` usa `HTMLAudioElement` via `kotlinx-browser`
- [ ]  `navigator.mediaSession` com `MediaMetadata` e action handlers
- [ ]  Gerenciar política de autoplay do browser (exigir gesto do usuário)
- [x]  `onended` para auto-play do próximo episódio da fila

---

### 4.3 PlayerViewModel (commonMain)

```kotlin
class PlayerViewModel(
    private val audioPlayer: AudioPlayer,
    private val savePlaybackPositionUseCase: SavePlaybackPositionUseCase,
    private val getPlaybackStateUseCase: GetPlaybackStateUseCase
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = audioPlayer.playerState

    init {
        // Restaurar último estado ao iniciar
        viewModelScope.launch {
            getPlaybackStateUseCase()?.let { saved ->
                // Preparar episódio na posição salva sem iniciar reprodução
            }
        }

        // Auto-save da posição a cada 5 segundos
        viewModelScope.launch {
            playerState
                .map { it.position }
                .distinctUntilChanged()
                .debounce(5_000)
                .collect { position ->
                    playerState.value.currentEpisode?.let { ep ->
                        savePlaybackPositionUseCase(ep.id, position)
                    }
                }
        }
    }

    fun play(episode: Episode) { viewModelScope.launch { audioPlayer.play(episode) } }
    fun pause()                { audioPlayer.pause() }
    fun seekTo(ms: Long)       { audioPlayer.seekTo(ms) }
    fun setSpeed(speed: Float) { audioPlayer.setSpeed(speed) }
    fun skipForward()          { audioPlayer.skipForward(30) }
    fun skipBackward()         { audioPlayer.skipBackward(15) }

    override fun onCleared()   { audioPlayer.release() }
}
```

**Use Cases do Player:**

```
PlayEpisodeUseCase
PausePlaybackUseCase
SeekToPositionUseCase
SetPlaybackSpeedUseCase
SavePlaybackPositionUseCase    ← persiste no Room 3 (PlaybackStateEntity)
GetPlaybackStateUseCase        ← restaura ao abrir o app
AddToQueueUseCase
RemoveFromQueueUseCase
SetSleepTimerUseCase
```

---

## Fase 5 — Interface (Compose Multiplatform 1.10.3)

> ⏱️ **Estimativa:** 3 semanas

### Objetivo

Implementar todas as telas com Compose Multiplatform, design system e navegação com Decompose.

---

### 5.1 Design System

```kotlin
// Theme.kt (commonMain)
@Composable
fun PodcastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography   = PodcastTypography,
        content      = content
    )
}
```

- [ ]  Paleta de cores customizada (dark/light)
- [ ]  Tipografia com escala Material 3
- [ ]  Dimensões e espaçamentos em `Dimensions.kt`
- [ ]  Extração de cor dominante do artwork do podcast:
    - Android: `Palette API` (`androidx.palette:palette:1.0.0`)
    - iOS: `Core Image` / `UIImage` via interop
    - Desktop/Web: algoritmo de quantização em Kotlin puro

---

### 5.2 Navegação com Decompose 3.3.0

```kotlin
// RootComponent.kt
interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        class Library(val component: LibraryComponent)           : Child()
        class PodcastDetail(val component: PodcastDetailComponent) : Child()
        class Player(val component: PlayerComponent)             : Child()
        class Search(val component: SearchComponent)             : Child()
    }
}
```

Layout adaptativo por form factor:

| Plataforma | Navegação |
|---|---|
| Android / iOS (portrait) | `BottomNavigationBar` |
| Tablet / Desktop | `NavigationRail` ou `PermanentNavigationDrawer` |
| Web (< 600dp) | `BottomNavigationBar` |
| Web (≥ 600dp) | `NavigationRail` |

---

### 5.3 Telas e UiState

**LibraryScreen**
```kotlin
data class LibraryUiState(
    val podcasts: List<Podcast> = emptyList(),
    val isLoading: Boolean     = false,
    val isRefreshing: Boolean  = false,
    val addFeedDialog: AddFeedDialogState? = null,
    val error: PodcastError?   = null
)
data class AddFeedDialogState(
    val url: String = "",
    val isValidating: Boolean = false,
    val preview: Podcast? = null,
    val error: PodcastError? = null
)
```

**PodcastDetailScreen**
```kotlin
data class PodcastDetailUiState(
    val podcast: Podcast? = null,
    val episodes: List<Episode> = emptyList(),
    val filter: EpisodeFilter = EpisodeFilter.ALL,
    val isRefreshing: Boolean = false
)
enum class EpisodeFilter { ALL, UNPLAYED, DOWNLOADED }
```

**PlayerScreen**
- Artwork grande · Seek bar com `posição / duração`
- Voltar 15s · Play/Pause · Avançar 30s
- Seletor de velocidade: 0.75x, 1.0x, 1.25x, 1.5x, 1.75x, 2.0x
- Sleep timer: 15min, 30min, 45min, 60min, fim do episódio
- Fila de reprodução (BottomSheet com `LazyColumn`)

**SearchScreen**
- `SearchBar` com debounce de 300ms no ViewModel
- Busca por título e descrição nos episódios locais
- Filtros: podcast específico / data / estado de reprodução

---

### 5.4 Componentes Compartilhados

```
EpisodeListItem         PodcastCard           MiniPlayer
ProgressSlider          SpeedSelector         SleepTimerDialog
AddFeedDialog           LoadingOverlay        ErrorSnackbar
AsyncPodcastImage       UnplayedBadge         PartialProgressBar
```

---

## Fase 6 — Ajustes por Plataforma

> ⏱️ **Estimativa:** 2 semanas

### 6.1 Android

- [ ]  Notificação de playback com **Media3 1.5.1** (`DefaultMediaNotificationProvider`)
- [ ]  **Glance Widget** para home screen (4x1 e 4x2) com `GlanceAppWidget`
- [ ]  **Android Auto** com `MediaLibraryService` e browse tree de episódios
- [ ]  **Edge-to-edge** com `WindowCompat.setDecorFitsSystemWindows()`
- [ ]  **Predictive back gesture** com `BackHandler` do Decompose
- [ ]  Adaptive icons (foreground + background layers)

### 6.2 iOS

- [ ]  **WidgetKit**: small (artwork + título), medium (+ controles), lock screen
- [ ]  **CarPlay** com `CPNowPlayingTemplate`
- [ ]  **Siri Shortcuts**: "Continue ouvindo", "Mostrar novos episódios"
- [ ]  **Spotlight indexing** com `CSSearchableItem`
- [ ]  **Live Activity** (Dynamic Island) via `ActivityKit`
- [ ]  Layout de duas colunas no **iPadOS** com `SplitView`

### 6.3 Desktop

- [ ]  **System Tray** com `java.awt.SystemTray` — play/pause, pular, sair
- [ ]  **Atalhos de teclado globais** com `JIntellitype` ou `JNativeHook`:
    - `Space` — Play/Pause
    - `Ctrl/Cmd + →` — Avançar 30s / `Ctrl/Cmd + ←` — Voltar 15s
    - `Ctrl/Cmd + ↑/↓` — Velocidade
- [ ]  **macOS**: menu bar nativo via `NSMenu`, integração com media keys
- [ ]  **Linux**: MPRIS2 via D-Bus para KDE/GNOME
- [ ]  **Drag & drop** de arquivo `.opml` direto na janela

### 6.4 Web

- [ ]  **PWA Manifest** (`manifest.json`) com ícones, `display: standalone`, `theme_color`
- [ ]  **Service Worker** para cache da UI shell e artworks
- [ ]  **Media Session API** para media keys do browser e mini-player nativo
- [ ]  **Share Target API** para receber URLs de outros apps
- [ ]  Layout responsivo com breakpoints (mobile < 600px, tablet < 1024px)
- [ ]  Deploy estático em **Vercel** ou **Netlify** com output `wasmJsBrowserDistribution`

---

## Fase 7 — Testes, QA e Deploy

> ⏱️ **Estimativa:** 2 semanas + manutenção contínua

### 7.1 Testes Unitários

Ferramentas: `kotlin.test` · `kotlinx-coroutines-test 1.10.2` · `Turbine 1.2.0` · `MockK 1.14.0`

```kotlin
// AddPodcastFromUrlUseCaseTest.kt
class AddPodcastFromUrlUseCaseTest {
    private val rssDataSource   = mockk<RssFeedDataSource>()
    private val podcastRepo     = mockk<PodcastRepository>()
    private val episodeRepo     = mockk<EpisodeRepository>()
    private val useCase         = AddPodcastFromUrlUseCase(rssDataSource, podcastRepo, episodeRepo)

    @Test
    fun `returns InvalidUrl for malformed url`() = runTest {
        val result = useCase("not-a-url")
        assertTrue(result.isFailure)
        assertIs<PodcastError.InvalidUrl>(result.exceptionOrNull())
    }

    @Test
    fun `saves podcast on successful fetch`() = runTest {
        coEvery { podcastRepo.getByFeedUrl(any()) } returns null
        coEvery { rssDataSource.fetchFeed(any()) } returns Result.success(fakeFeed)
        coEvery { podcastRepo.save(any()) } just Runs
        coEvery { episodeRepo.insertAll(any()) } just Runs

        val result = useCase("https://feeds.example.com/podcast.rss")
        assertTrue(result.isSuccess)
        coVerify { podcastRepo.save(any()) }
        coVerify { episodeRepo.insertAll(any()) }
    }
}

// PlayerViewModelTest.kt — Turbine para StateFlow
class PlayerViewModelTest {
    @Test
    fun `emits playing state after play`() = runTest {
        val vm = PlayerViewModel(fakeAudioPlayer, fakeSaveUseCase, fakeGetUseCase)
        vm.playerState.test {
            vm.play(fakeEpisode)
            val state = awaitItem()
            assertTrue(state.isPlaying)
            assertEquals(fakeEpisode, state.currentEpisode)
        }
    }
}
```

**Cobertura mínima:** 80% na camada de domínio (use cases + models)

---

### 7.2 Testes de Integração

```kotlin
// PodcastDaoTest.kt (androidTest) — Room 3 in-memory
@RunWith(AndroidJUnit4::class)
class PodcastDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: PodcastDao

    @Before
    fun setup() {
        // Room 3 — inMemoryDatabaseBuilder não requer Context obrigatório
        db = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(UnconfinedTestDispatcher())
            .build()
        dao = db.podcastDao()
    }

    @Test
    fun insertAndRetrieve() = runTest {
        dao.insert(fakePodcastEntity)
        val result = dao.getById(fakePodcastEntity.id)
        assertEquals(fakePodcastEntity, result)
    }

    @After
    fun teardown() { db.close() }
}
```

- [ ]  Testes de todos os DAOs com banco in-memory
- [ ]  Testes de integração Repositório + DAO
- [ ]  Testes do RSS parser com arquivos XML de fixture (feeds reais salvos)

---

### 7.3 Testes de UI (Compose)

```kotlin
// LibraryScreenTest.kt
class LibraryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun showsEmptyState() {
        compose.setContent {
            PodcastTheme {
                LibraryScreen(
                    uiState  = LibraryUiState(podcasts = emptyList()),
                    onEvent  = {}
                )
            }
        }
        compose.onNodeWithText("Nenhum podcast adicionado").assertIsDisplayed()
        compose.onNodeWithContentDescription("Adicionar podcast").assertIsDisplayed()
    }
}
```

- [ ]  Screenshot tests com **Paparazzi** (Android) para regressão visual
- [ ]  Testes de acessibilidade com `SemanticsNodeInteraction`

---

### 7.4 Pipeline CI/CD (GitHub Actions)

```yaml
# .github/workflows/ci.yml
name: CI / CD

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test-shared:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - name: Unit Tests (shared)
        run: ./gradlew :shared:kspCommonMainKotlinMetadata :shared:testDebugUnitTest
      - name: Detekt
        run: ./gradlew detekt

  build-android:
    needs: test-shared
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: ./gradlew :androidApp:bundleRelease
      - uses: actions/upload-artifact@v4
        with: { name: android-aab, path: androidApp/build/outputs/bundle/release/ }

  build-desktop:
    needs: test-shared
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: ./gradlew :desktopApp:packageDistributionForCurrentOS

  build-web:
    needs: test-shared
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: ./gradlew :webApp:wasmJsBrowserDistribution
      - uses: actions/upload-artifact@v4
        with:
          name: web-dist
          path: webApp/build/dist/wasmJs/productionExecutable

  deploy-web:
    needs: build-web
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/download-artifact@v4
        with: { name: web-dist, path: dist }
      - uses: amondnet/vercel-action@v25
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
          vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID }}
          working-directory: dist
```

---

### 7.5 Publicação

| Plataforma | Formato | Destino | Automação |
|---|---|---|---|
| Android | `.aab` | Google Play (Internal → Beta → Production) | Fastlane `supply` |
| iOS | `.ipa` | TestFlight → App Store Connect | Fastlane `pilot` |
| Windows | `.exe` (NSIS) | GitHub Releases | `packageMsi` task |
| macOS | `.dmg` | GitHub Releases | `packageDmg` task |
| Linux | `.deb` / `.AppImage` | GitHub Releases | `packageDeb` task |
| Web | Wasm estático | Vercel / Netlify | GitHub Actions |

---

## 📌 Convenções de Código

### Nomenclatura

| Elemento | Padrão | Exemplo |
|---|---|---|
| UseCase | `VerbNounUseCase` | `AddPodcastFromUrlUseCase` |
| Repository (interface) | `NounRepository` | `PodcastRepository` |
| Repository (impl) | `NounRepositoryImpl` | `PodcastRepositoryImpl` |
| DataSource | `NounDataSource` | `RssFeedDataSource` |
| ViewModel | `ScreenViewModel` | `LibraryViewModel` |
| Entity (Room 3) | `NounEntity` | `PodcastEntity` |
| UiState | `ScreenUiState` | `LibraryUiState` |
| Screen Composable | `NounScreen` | `LibraryScreen` |

### Regras Gerais

- [ ]  Nunca expor tipos do `androidx.room3` fora da camada `data`
- [ ]  ViewModels nunca importam classes do módulo `data` diretamente
- [ ]  Usar `Result<T>` para operações que podem falhar nos use cases
- [ ]  `Flow` para dados reativos (DAOs), `suspend fun` para operações pontuais
- [ ]  **Todos** os DAOs Room 3 devem ser `suspend` ou retornar `Flow` — sem exceções
- [ ]  `expect/actual` apenas para adapters de plataforma — nunca para regras de negócio
- [ ]  Toda string voltada ao usuário em `strings.xml` / `Localizable.strings`

---

*Gerado para o projeto KMP Podcast App — Room 3 + Ktor 3 + Koin 4 + Compose Multiplatform 1.10.3 — Maio 2026*
