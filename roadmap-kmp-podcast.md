# 🎙️ Roadmap — App de Podcast KMP

> **Stack:** Kotlin Multiplatform · Compose Multiplatform · Room 3 · Ktor 3 · Koin 4 · MVVM + Clean Architecture
> **Plataformas:** Android · iOS · Desktop (Windows/macOS/Linux) · Web (WasmJs)
> **Atualizado em:** Maio 2026 (Refletindo o progresso atual do desenvolvimento)

---

## ✅ Progresso Atual (Maio 2026)

Atualmente, o projeto superou os estágios iniciais de infraestrutura e possui uma base sólida de lógica compartilhada com alta cobertura de testes.

### Concluído e Validado
*   **Setup Cross-Platform:** Android, iOS (Arm64/Simulator), Desktop (JVM) e Web (WasmJs) configurados.
*   **Camada de Dados (Room 3):** Banco de dados Room 3 implementado com suporte a drivers nativos e persistência OPFS na Web.
*   **Infraestrutura de Testes Multiplataforma:**
    *   Migração de 100% dos testes unitários para `commonTest`.
    *   Implementação de Fakes robustos (Repositório, DataSource, Player).
    *   Validação de banco de dados real (Room) no simulador iOS.
    *   Build de CI verde em todas as plataformas (com skip controlado no Wasm para DB).
*   **Player de Áudio:** Refatorado para `interface` com suporte a Fakes e implementações nativas (Media3, AVFoundation, JavaFX, HTML5).

---

## 📐 Arquitetura de Referência

```
:androidApp  :iosApp  :desktopApp  :webApp
        ↓        ↓         ↓          ↓
              :shared
     ┌────────────────────────────┐
     │  Presentation (ViewModel)  │  ← StateFlow, UiState, side-effects compartilhados
     │  Domain (UseCases/Entities)│  ← Pure Kotlin, lógica de negócio única
     │  Data (Repo/Sources/DAO)   │  ← Room 3, Ktor 3, RSS Parser (validado em todas as plataformas)
     └────────────────────────────┘
```

---

## 🛠️ Cronograma de Desenvolvimento (Fases)

### Fase 1 — Setup e Infraestrutura Core (CONCLUÍDO ✅)
*   [x] Configuração `libs.versions.toml` com versões estáveis (Kotlin 2.3.21, Compose 1.11.0, Room 3.0.0-alpha05).
*   [x] Configuração do módulo `:shared` com targets Android, iOS, Desktop e Wasm.
*   [x] Implementação do `createAppDatabase` com drivers específicos (BundledSQLite vs WebWorker).
*   [x] Implementação do `AudioPlayer` como interface compartilhada.
*   [x] Configuração de CI/CD via GitHub Actions.

### Fase 2 — Camada de Dados e Lógica de Negócio (CONCLUÍDO ✅)
*   [x] Entidades e DAOs do Room 3 (Podcasts, Episodes, PlaybackState).
*   [x] Parsing de Feed RSS com suporte ao namespace iTunes.
*   [x] Implementação de Use Cases (AddPodcast, Refresh, Delete, Stats).
*   [x] Testes de Integração do DAO rodando no iOS e Desktop.
*   [x] 100% da lógica de negócio testada no `commonTest`.

### Fase 3 — Player de Áudio e Persistência de Estado (CONCLUÍDO ✅)
*   [x] Implementação `AndroidAudioPlayer` (Media3/ExoPlayer).
*   [x] Implementação `IosAudioPlayer` (AVFoundation).
*   [x] Implementação `DesktopAudioPlayer` (JavaFX).
*   [x] Implementação `WasmAudioPlayer` (HTML5 Audio).
*   [x] Persistência automática de posição de áudio no banco de dados.
*   [x] Suporte a Fila de Reprodução (Queue) em todas as plataformas.
*   [x] Sleep Timer multiplataforma (`setSleepTimer` implementado).

### Fase 4 — Interface de Usuário (Compose Multiplatform) (CONCLUÍDO ✅)
*   [x] Navegação básica com **Decompose** (`RootComponent`).
*   [x] Componentes Core (PodcastCard, EpisodeListItem, AsyncImage).
*   [x] Telas Principais:
    *   [x] Library (`LibraryScreen` via `LazyVerticalGrid`).
    *   [x] Search (`SearchScreen`).
    *   [x] Podcast Detail (`PodcastDetailScreen`).
    *   [x] Episode Detail (`EpisodeDetailScreen`).
    *   [x] Downloads (`DownloadedEpisodesScreen`).
    *   [x] Player (MiniPlayer e Tela Cheia via `PlayerScreen`).
*   [x] Layout Adaptativo (ListDetailPaneScaffold e NavigationSuiteScaffold).
*   [x] Refinamento do Design System (Cores, Tipografia Material 3).
*   [x] Animações complexas de transição entre MiniPlayer e Player expandido.

### Fase 5 — Integrações Nativas e Polimento (CONCLUÍDO ✅)
*   [x] **iOS:** Background Audio, Now Playing Integration e Lock Screen Controls.
*   [x] **Android:** Notificações Media3 com suporte a Skip Forward/Backward e MediaSession.
*   [x] **Android:** Android Auto (metadados e controles personalizados).
*   [x] **Desktop:** System Tray, atalhos globais de teclado, integração com Menu Bar.
*   [x] **Web:** Media Session API.
*   [x] **Acessibilidade:** Suporte a Screen Readers e Touch Targets (min 48dp) em componentes core.

### Fase 6 — Internacionalização e Padronização de Constantes (CONCLUÍDO ✅)
*   [x] **Extração de Strings:** Remover todas as strings hardcoded e migrar para `composeResources` (strings.xml).
*   [x] **Suporte Multi-idioma:** Implementar traduções completas para:
    *   [x] Português (Brasil) - `pt-BR`.
    *   [x] Inglês - `en`.
    *   [x] Espanhol - `es`.
*   [x] **Padronização de Dimensões:** Criar um objeto de design system (ex: `AppDimensions`) para centralizar espaçamentos (dp), raios de borda e tamanhos de ícones.
*   [x] **Constantes de Negócio:** Centralizar valores numéricos (timeouts, intervalos de skip, limites de cache) em arquivos de configuração apropriados.
*   [x] **Formatadores Localizados:** Garantir que datas e durações usem o locale do sistema para exibição (via `Formatters.kt`).

### Fase 7 — Telemetria, Monitoramento e Observabilidade (PRÓXIMOS PASSOS 🚀)
*   [x] **Logging Multiplataforma (Kermit):** Substituir logs manuais/println pelo **Kermit (Touchlab)** no código compartilhado para melhor depuração em produção e integração com logs nativos.
*   [ ] **Firebase Analytics:** Implementar rastreamento de eventos de engajamento (play, pause, busca, assinaturas) no Android e iOS via wrapper KMP.
*   [ ] **Firebase Crashlytics:** Configurar a captura de crashes nativos (C/C++, Swift) e exceções Kotlin não tratadas para garantir estabilidade em todas as plataformas.
*   [ ] **Firebase Performance Monitoring:** Monitorar tempos de resposta de rede (Ktor), carregamento de imagens e o tempo de inicialização "Time to First Render".
*   [ ] **Firebase Remote Config:** Implementar controle remoto de flags de funcionalidades e constantes de negócio (ex: intervalos de skip) sem necessidade de novo deploy.
*   [ ] **Análise Estática (Detekt):** Configurar o **Detekt** para garantir a qualidade do código Kotlin e manter padrões arquiteturais consistentes em todos os módulos.

### Fase 8 — Expansão da Base de Testes e Qualidade
*   [ ] **Testes de UI (Compose Test):** Implementar testes de interação para as principais telas em `commonTest`.
*   [ ] **Cobertura de Casos de Borda:** Expandir testes de domínio para lidar com feeds RSS malformados e interrupções de rede.
*   [ ] **Benchmarks de Performance:** Medir tempos de carregamento do banco de dados e parsing de XML.
*   [ ] **Testes de Regressão Visual:** Configurar infraestrutura de screenshots para detectar mudanças inesperadas na UI.
*   [ ] **Testes de Estresse do Player:** Validar comportamento sob condições extremas de buffering e troca rápida de faixas.

---

## 📌 Convenções e Engineering Standards (Atualizado)

### Testes
*   **Regra de Ouro:** Todos os novos ViewModels e Use Cases **devem** ser testados em `commonTest`.
*   **Fakes:** Preferir Fakes manuais em vez de MockK para garantir compatibilidade com Native/Wasm.
*   **Banco de Dados:** Usar `createInMemoryDatabase()` para testes de integração de DAO no `commonTest`.

### Gerenciamento de Dependências
*   **SQLite:** Usar `libs.sqlite.bundled` para targets nativos e `libs.sqlite.web` para Wasm.
*   **Ktor:** Versão 3.5.0+ para estabilidade em streaming de áudio.

---
*Podcast App KMP — Construindo o futuro do podcasting multiplataforma.*
