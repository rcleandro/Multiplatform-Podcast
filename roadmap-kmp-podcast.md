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

### Fase 3 — Player de Áudio e Persistência de Estado (EM PROGRESSO 🔄)
*   [x] Implementação `AndroidAudioPlayer` (Media3/ExoPlayer).
*   [x] Implementação `IosAudioPlayer` (AVFoundation).
*   [x] Implementação `DesktopAudioPlayer` (JavaFX).
*   [x] Implementação `WasmAudioPlayer` (HTML5 Audio).
*   [x] Persistência automática de posição de áudio no banco de dados.
*   [ ] Suporte a Fila de Reprodução (Queue) avançada em todas as plataformas.
*   [ ] Sleep Timer multiplataforma.

### Fase 4 — Interface de Usuário (Compose Multiplatform) (EM PROGRESSO 🔄)
*   [x] Navegação básica com **Decompose**.
*   [x] Componentes Core (PodcastCard, EpisodeListItem, AsyncImage).
*   [x] Telas Principais:
    *   [x] Library (Biblioteca de Podcasts).
    *   [x] Search (Busca local e remota).
    *   [x] Podcast Detail.
    *   [x] Episode Detail.
    *   [x] Downloads.
    *   [x] Player (MiniPlayer e Tela Cheia).
*   [ ] Refinamento do Design System (Cores, Tipografia Material 3).
*   [ ] Extração dinâmica de cores do Artwork.

### Fase 5 — Integrações Nativas e Polimento (PRÓXIMOS PASSOS 🔜)
*   [ ] **iOS:** WidgetKit, Live Activities, integração com Apple Watch.
*   [ ] **Android:** Notificações Media3, Android Auto, Glance Widgets.
*   [ ] **Desktop:** System Tray, atalhos globais de teclado, integração com Menu Bar.
*   [ ] **Web:** PWA, Service Workers para offline, Media Session API.
*   [ ] **Acessibilidade:** Suporte a Screen Readers em todas as plataformas.

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
