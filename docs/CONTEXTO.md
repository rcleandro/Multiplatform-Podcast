# Podcast KMP — Contexto do Projeto

> Retrato do código em `main` (commit `9e4f898`, 24/05/2026), escrito na retomada do projeto em 02/10/2026.
> O plano de trabalho está em [ROADMAP_MELHORIAS.md](ROADMAP_MELHORIAS.md); o roadmap original (fases 1–8) em
> [`roadmap-kmp-podcast.md`](roadmap-kmp-podcast.md).

---

## 1. O que é

Player de podcasts multiplataforma com uma base única em Kotlin Multiplatform + Compose Multiplatform, para
**Android** (com Android Auto), **iOS**, **Desktop** (macOS/Windows/Linux) e **Web** (Wasm).

O usuário adiciona podcasts **colando a URL do feed RSS**. O app baixa e interpreta o feed, guarda podcast e
episódios num banco local e toca os episódios por streaming ou a partir de um download. Não há conta, backend próprio
nem sincronização entre dispositivos: tudo fica no aparelho. Firebase (Analytics e Crashlytics) roda só no Android e no
iOS; Remote Config e Performance saíram em 03/10/2026 (roadmap 10.11).

Funcionalidades que existem hoje:

- Biblioteca em grade com as capas, remoção do podcast e atualização de todos os feeds.
- Detalhe do podcast com filtros (todos / não ouvidos / baixados), "marcar como ouvido" e "marcar anteriores como ouvidos".
- Detalhe do episódio com a descrição em HTML.
- Busca, que procura **só nos episódios já salvos** (não existe busca em diretório de podcasts).
- Downloads com progresso, e a tela "Baixados".
- Player com mini player, tela cheia, velocidade, avanço e retrocesso, fila, sleep timer e retomada da posição.
- Integrações nativas: MediaSession/notificação e Android Auto; Now Playing e controles na tela de bloqueio do iOS;
  bandeja do sistema no Desktop; Media Session API na Web.
- Textos em `en`, `pt` e `es`.

## 2. Stack

| Área | Tecnologia | Versão |
|---|---|---|
| Linguagem | Kotlin Multiplatform | 2.3.21 |
| UI | Compose Multiplatform + Material 3 + Material 3 Adaptive | 1.11.0 / 1.9.0 / 1.2.0 |
| Navegação | Decompose (`childStack`) | 3.5.0 |
| DI | Koin (`koin-compose-viewmodel`) | 4.2.1 |
| Banco | Room 3 KMP (**alpha**) + SQLite bundled / sqlite-web (OPFS) | 3.0.0-alpha05 / 2.7.0-alpha05 |
| Paginação | AndroidX Paging (common + compose) | 3.5.0 |
| Rede | Ktor client (Android, Darwin, CIO, Js) | 3.5.0 |
| Arquivos | Okio (`FakeFileSystem` na Web) | 3.17.0 |
| Imagens | Coil 3 com fetcher Ktor | 3.4.0 |
| Logs | Kermit, espelhado no Crashlytics por `AppLogger` | 2.1.0 |
| Firebase | Analytics e Crashlytics: GitLive SDK (Android/iOS) + CocoaPods no iOS | 2.4.0 |
| Player | Media3 (Android), AVFoundation (iOS), JavaFX Media (Desktop), `<audio>` HTML5 (Web) | 1.10.1 / — / 21.0.5 / — |
| Testes | kotlin-test, coroutines-test, Turbine, Ktor MockEngine, Compose ui-test, MockK (só JVM) | — |
| Qualidade | Detekt (com baseline), Kover (só relatório, sem meta) | 1.23.8 / 0.9.8 |
| Build | Gradle (configuration cache e build cache ligados), AGP 9.2.1, `compileSdk`/`targetSdk` 37, `minSdk` 26, JDK 21 no CI | — |

## 3. Mapa de módulos

Só existe **um módulo de código**, o `:shared`, que concentra domínio, dados, UI e as implementações de plataforma.
Os apps são cascas finas.

| Módulo | Conteúdo |
|---|---|
| `:shared` | Tudo (detalhes abaixo). Gera o framework `Shared` para o iOS via CocoaPods e o `Res` dos recursos em `br.com.carvalho.podcast.shared`. |
| `:androidApp` | `PodcastApplication` (inicia Koin e `AppContext`), `MainActivity` (pede `POST_NOTIFICATIONS` e monta o `RootComponentImpl`), manifest com o `PodcastMediaService` e o descritor do Android Auto. |
| `:desktopApp` | `Main.kt`: janela, bandeja do sistema (textos em inglês no código) e atalhos. |
| `:webApp` | `Main.kt` Wasm e a task `copySqliteWorker`, que copia os arquivos do sqlite-web para o bundle. |
| `iosApp/` | Projeto Xcode + Podfile; `ContentView` embrulha o `MainViewController()` do Kotlin. |

Pacotes dentro de `:shared/src/commonMain/kotlin/br/com/carvalho/podcast`:

| Pacote | O que tem |
|---|---|
| `core/` | `AppConfig` (constantes lidas do Remote Config com fallback), `designsystem/` (cores, tipografia, `AppDimensions`, shapes, `PodcastTheme`), `di/` (módulos Koin), `network/` (`createHttpClient` expect/actual, `commonJson`), `image/` (Coil), `analytics/` e `crashlytics/` (`expect object`), `util/` (`AppLogger`, `AppContext`, `FileUtils`, `CoroutineDispatchers`, `getCurrentTimestamp`), `extensions/` (formatação de data e duração). |
| `domain/` | `model/` (`Podcast`, `Episode`, `PlayerState`, `PodcastError`), `repository/` (interfaces), `usecase/` (`AddPodcastFromUrl`, `RefreshPodcast`, `DeletePodcast`, `GetLibraryStats`), `player/AudioPlayer` (interface + `expect fun createAudioPlayer()`), `download/EpisodeDownloader` (interface + `DownloadStatus`). |
| `data/` | `local/` (Room: `AppDatabase` v3, entidades e DAOs), `remote/` (`RssFeedDataSourceImpl`, `RssXmlParser` feito à mão), `mapper/` (RSS → domínio, entidade ↔ domínio), `repository/` (`PodcastRepositoryImpl`, `PlayerRepositoryImpl`, `EpisodePagingSource`), `download/KtorEpisodeDownloader`. |
| `feature/<nome>/presentation` | Uma tela + um ViewModel por feature: `library`, `podcast`, `episode`, `search`, `downloads`, `player`. |
| `presentation/` | `navigation/` (`RootComponent`, `RootComponentImpl`, `RootContent`) e `component/` (`PodcastCard`, `EpisodeListItem`, `MiniPlayer`, `HtmlText`). |

Por plataforma (`androidMain`, `iosMain`, `desktopMain`, `wasmJsMain`): `AudioPlayer`, banco, `FileUtils`,
`HttpClient`, Koin (`initKoin`) e os wrappers de Firebase (no Desktop e na Web, no-op). No Android também ficam o
`PodcastMediaService` (Media3 `MediaLibraryService`, com a árvore de navegação do Android Auto); no iOS, a
`IosAudioSession`; na Web, a `WebMediaSession`.

## 4. Fluxos principais

### 4.1 Inicialização
`initKoin()` (por plataforma) sobe os módulos de `core/di/Modules.kt`; no Android e no iOS, uma coroutine solta
inicializa o Firebase e faz `RemoteConfig.fetchAndActivate()`. O banco é criado já no início (`createdAtStart`).
`App(root)` registra o `ImageLoader` do Coil e aplica `PodcastTheme`.

### 4.2 Adicionar podcast
`LibraryViewModel.addPodcast()` põe `https://` na frente se faltar → `AddPodcastFromUrlUseCase` recusa se já
existir um podcast com aquele id → `RssFeedDataSource.fetchFeed` (GET + `RssXmlParser.parse` em `Dispatchers.Default`)
→ mappers → `savePodcast` (upsert) + `saveEpisodes` (`INSERT OR IGNORE`).
**Identidade:** o id do podcast é a URL do feed; o id do episódio é o `<guid>` do item (ou o hash do título, se não
houver guid), sem o podcast na chave.

### 4.3 Atualizar
`RefreshPodcastUseCase` refaz o fetch e salva de novo; como os episódios são `INSERT OR IGNORE`, só entram os novos,
e os existentes nunca são atualizados. `refreshAll()` percorre a biblioteca em série.

### 4.4 Reprodução
`PlayerViewModel` (um por `ViewModelStoreOwner`, compartilhado entre `RootContent` e `PlayerScreen`) delega ao
`AudioPlayer` singleton. Cada plataforma publica `PlayerState` atualizando a posição a cada 500 ms. O ViewModel salva
o estado (`playback_state`, uma linha com a fila em JSON) e o progresso do episódio, e marca o episódio como ouvido
acima de 95% da duração. Ao abrir, restaura episódio, posição, fila e velocidade. O sleep timer é um laço no
`viewModelScope`.

### 4.5 Downloads
`KtorEpisodeDownloader` baixa para `<baseDir>/downloads/<episode.id>.mp3`, publica `DownloadStatus` num `StateFlow`
e marca `isDownloaded` no banco. O player usa o arquivo local se `getLocalPath` o encontrar. Na Web, o "disco" é um
`FakeFileSystem` em memória.

### 4.6 Navegação
`RootComponentImpl` mantém uma pilha Decompose com abas (Library, Search, Downloads, Player) e detalhes. O
`RootContent` traduz a pilha para um `NavigationSuiteScaffold` (barra ou rail conforme a largura) com
`ListDetailPaneScaffold` (lista / podcast / episódio), o mini player animado e o player em tela cheia por cima.

## 5. Banco de dados (Room 3, versão 3)

| Tabela | Chave | Observações |
|---|---|---|
| `podcasts` | `id` = URL do feed | `categories` serializado em string; `isSubscribed` é sempre `true` |
| `episodes` | `id` = guid | FK para `podcasts` com `CASCADE`; índices em `podcastId` e `publishDate`; guarda `isPlayed`, `playbackPosition` e `isDownloaded` |
| `playback_state` | `id` = 1 | episódio atual, posição, velocidade e a fila inteira em JSON |

Os esquemas 1–3 estão exportados em `core/database/schemas/`, mas **nenhuma migração existe**: todas as plataformas usam
`fallbackToDestructiveMigration(true)`.

## 6. Constantes do app (`AppConfig`)

Até 03/10/2026 vinham do Firebase Remote Config com fallback local; o Remote Config saiu (10.11) e os fallbacks
viraram constantes: avanço 30 s, retrocesso 10 s, debounce do salvamento 2000 ms, "ouvido" a 95%, tick do timer
1000 ms, debounce da busca 300 ms, buffer de download 8192, velocidades 0,5–2,5. Sobra só a interface `firebase-config-interop` /
`FirebaseRemoteConfigInterop`, que o Crashlytics declara; ela não contém o SDK nem chama o serviço.

## 7. Design system atual

Fica em `core/designsystem`:

- **Cores:** paleta monocromática (preto/branco/cinzas) nos dois temas, mais o vermelho de erro do M3. Não há cor de
  marca nem cores semânticas (tocando, baixado, ouvido).
- **Tipografia:** a escala do M3 reescrita com `FontFamily.Default`, sem fonte própria.
- **Dimensões:** `AppDimensions`, com 60 tokens que se repetem (`paddingSmall` = `spacingSmall` = 4dp, `paddingNormal`
  = `spacingLarge` = 16dp…) e que misturam espaçamento, tamanhos de componente, breakpoints, colunas da grade e opacidades.
- **Componentes:** `PodcastCard`, `EpisodeListItem`, `MiniPlayer` e `HtmlText` ficam em `presentation/component`,
  fora do design system. Não há estado vazio, estado de erro, botão de download nem botão de play padronizados.
- Não há snapshot tests nem referência visual.

## 8. Divergências entre a documentação e o código

| Documento diz | Código faz |
|---|---|
| Fase 6: "remover todas as strings hardcoded" (✅) | Mensagens de erro em pt-BR nos ViewModels (`LibraryViewModel`, `PodcastDetailViewModel`, `EpisodeDetailViewModel`, `DownloadedEpisodesViewModel`), em `LongExtensions.toDate()`, no `RssXmlParser` ("Sem título", "Podcast Desconhecido"), no `PodcastCard` ("Opções do podcast") e na bandeja do Desktop (em inglês). `app_name` e `rss_url_placeholder` faltam no `values-pt`. |
| Fase 6: datas formatadas pelo locale (✅) | `toDate()` devolve texto fixo em português e data `d/m/aaaa`. |
| `regras-negocio.md`: verificação de espaço em disco, validação de integridade e retomada de download | Nada disso existe; `resume()` é vazio. |
| `regras-negocio.md`: feed sem `<enclosure>` retorna `ParseFailed` | `ParseFailed` e `FetchFailed` nunca são lançados; item sem enclosure vira episódio com `audioUrl = ""`. |
| `regras-negocio.md`: migrações protegidas por testes em produção | `fallbackToDestructiveMigration(true)` em todas as plataformas e nenhum teste de migração. |
| `arquitetura.md`: módulos `:shared:core`, `:shared:data`… | São pacotes de um só módulo; o domínio importa `data.mapper` e `data.remote`. |
| README: JDK 17 ou 21 | CI usa 21; README não cita Firebase, CocoaPods nem os arquivos de segredo. |

`docs/arquitetura.md` e `docs/regras-negocio.md` estão no `.gitignore`: existem só nesta máquina.

## 9. Testes existentes

- **`commonTest` (rodam em todas as plataformas):** use cases, mappers, `RssXmlParser` (só fixtures pequenas), DAOs
  com banco real em memória (pulado na Web), `EpisodePagingSource`, `PodcastRepositoryImpl`,
  `KtorEpisodeDownloader` (MockEngine + `FakeFileSystem`), os 6 ViewModels (o do player com 48 linhas) e o `RootComponent`.
- **Fakes à mão:** `FakePodcastRepository`, `FakePlayerRepository`, `FakeAudioPlayer`, `FakeEpisodeDownloader`,
  `FakeRssFeedDataSource`, `FakeEpisodeDao`.
- **`desktopTest`:** `UserFlowIntegrationTest`, `MiniPlayerTest` e `PodcastCardTest` (Compose ui-test).
- Sem testes de: players nativos, `PodcastMediaService`, migrações, salvamento do progresso durante a reprodução,
  falha no meio do download, feeds reais.
- **CI** (`.github/workflows/ci.yml`): testes JVM (Android host + Desktop) e iOS (simulador) em paralelo, depois o
  build dos 4 alvos. **Não roda Detekt nem verifica a cobertura.**

## 10. Comandos

```bash
./gradlew :shared:desktopTest                 # testes mais rápidos (JVM)
./gradlew :shared:testAndroidHostTest         # testes no host Android
./gradlew :shared:iosSimulatorArm64Test       # testes no simulador iOS
./gradlew detekt                              # análise estática, sem baseline: todo achado barra o build
./gradlew :shared:koverHtmlReport             # relatório de cobertura

./gradlew :androidApp:installDebug
./gradlew :desktopApp:run
./gradlew :webApp:wasmJsBrowserDevelopmentRun
# iOS: cd iosApp && pod install, depois abrir iosApp.xcworkspace no Xcode
```

Arquivos locais fora do git: `androidApp/google-services.json`, `iosApp/iosApp/GoogleService-Info.plist` (no CI
vêm dos secrets `GOOGLE_SERVICES_JSON` / `GOOGLE_SERVICE_INFO_PLIST`) e `local.properties`.

## 11. Convenções

- Pacote raiz `br.com.carvalho.podcast`; código, identificadores e logs em inglês; textos de tela nos
  `composeResources` (`values`, `values-pt`, `values-es`).
- Commits em Conventional Commits sem ticket (`feat(shared): …`, `fix(ios): …`).
- `GEMINI.md` exige `./gradlew :shared:detekt` limpo antes de cada commit e mantém `MatchingDeclarationName`
  desligado por causa dos arquivos `.android.kt`/`.ios.kt`.
- Testes novos vão em `commonTest`, com fakes à mão em vez de MockK, para rodar no Native e no Wasm.
- Plataforma isolada por `expect`/`actual`; os SDKs nativos (Firebase etc.) são acessados por `expect object`.

## 12. Pontos de atenção

- Room 3 e sqlite-web estão em **alpha**; `skiko` é forçado para `0.9.43` no `shared/build.gradle.kts` para alinhar com o Coil no iOS.
- AGP 9 exigiu a task `syncComposeResourcesForAndroid` para empacotar os recursos do Compose no APK.
- O build do Desktop escolhe o classificador do JavaFX pela máquina que compila, então cada SO precisa do seu runner.
- A Web busca feeds RSS direto do navegador: qualquer feed sem cabeçalho CORS falha (é a maioria).
- `AppContext.context` é um global que lança exceção se for lido antes do `PodcastApplication.onCreate`.
