# ADR 0016 — App para Apple Watch

- **Status:** aceita (decisão do usuário em 02/11/2026, as seis recomendações)
- **Data:** 2026-11-02
- **Fase:** 25.14 do [roadmap](../ROADMAP_MELHORIAS.md) (era a 19.9c)

## Contexto

O Wear OS já tem um app próprio (19.9a–h): biblioteca, podcast, episódios, player com fila, velocidade, timer e
efeitos, downloads, listas inteligentes, tile e complicação, e sincronização com o telefone pelo Data Layer (19.9b:
`SyncSnapshot`, `planSync` e `LibrarySyncUseCase` em `:domain`, transporte no `WearableSync` do `:shared`). O
roadmap pediu, antes de qualquer código para o Apple Watch, que se confirmasse se o Room roda no watchOS e, se não
rodasse, que se escolhesse outra persistência.

**Investigação (02/11/2026).**

- **Bibliotecas.** Todas as dependências dos módulos de dados publicam watchOS nas versões em uso: Room 3
  (`room3-runtime` 3.1.0-alpha03: `watchosarm32`, `watchosarm64`, `watchosdevicearm64`, `watchossimulatorarm64`),
  `sqlite-bundled` 2.8.0-alpha03, Paging 3.5.3, Lifecycle ViewModel, Ktor 3.6.1 (`ktor-client-darwin`), Okio 3.18.2,
  xmlutil 1.0.2.1, multiplatform-settings 1.3.0, Koin 4.2.2, Kermit 2.2.0 e kotlinx-datetime 0.8.0.
- **Compilação de verdade.** Com `watchosSimulatorArm64()` acrescentado ao plugin `podcast.kmp.library` (experimento
  desfeito depois), compilaram para o watchOS `:core:common`, `:core:observability`, `:domain`, `:core:database`
  (com o KSP do Room gerando o banco), `:core:network`, `:data` e `:core:player`. Bastou: mover `iosMain` para
  `appleMain` (o grupo que vale para iOS e watchOS) nesses módulos, trocar `iosMain.dependencies` por
  `appleMain.dependencies` em `:core:database` e `:core:network`, somar `kspWatchosSimulatorArm64` ao Room e escrever
  um `PlatformCapabilities.watchos.kt`, porque o do iOS abre os Ajustes pelo `UIApplication`, que o watchOS não tem.
  O player do iOS compilou sem mudança: `AVPlayer`, `AVAudioSession` e `MediaPlayer` existem no watchOS.
- **O limite.** `:core:ui` e as sete features não compilam: dependem de
  `org.jetbrains.compose.components:components-resources` 1.12.2, que não publica watchOS ("Unresolved platforms:
  [watchosSimulatorArm64]"). Os ViewModels emitem `StringResource` (regra do `CLAUDE.md`), então nenhum deles chega
  ao relógio como está.
- **Fora do relógio.** O Firebase entra só pelo `:shared` (`firebaseMain`) e fica de fora. Esta máquina não tem
  runtime de watchOS instalado e o usuário tem um Galaxy Watch, não um Apple Watch: os testes visuais dependem do
  simulador, que precisa ser baixado (`xcodebuild -downloadPlatform watchOS`, alguns GB).

## Decisões

### 1. Persistência: o Room, com o mesmo banco do telefone

O Room serve. A persistência própria que o roadmap previa como plano B (SwiftData ou arquivos) fica descartada: ela
duplicaria esquema, migrações e regras que hoje são testadas uma vez em `commonTest`.

### 2. ViewModels: fachadas do relógio num módulo próprio

| Opção | A favor | Contra |
|---|---|---|
| **a. Módulo `:watch` com fachadas pequenas** (recomendada) | O relógio mostra pouco (como o Wear OS); estados simples, com os textos como chaves que o Swift resolve no `Localizable.xcstrings` já gerado; não mexe nas features | Uma segunda camada de apresentação, menor, para o relógio |
| b. Tirar `StringResource` dos ViewModels | Os mesmos ViewModels nas seis plataformas | Toca todas as telas e as cinco UIs, para destravar uma só |
| c. Esperar o Compose Resources para watchOS | Nada a fazer agora | Sem data; o relógio fica parado |

As fachadas usam os casos de uso e repositórios de `:domain` e `:data` diretamente, como o `CarCatalog` (25.13) e o
`playerWidgetState` (25.12) já fazem, e ficam testadas em `commonTest`.

### 3. Alvos e fontes

O watchOS entra só nos módulos que o relógio usa (`:core:common`, `:core:observability`, `:domain`,
`:core:database`, `:core:network`, `:data`, `:core:player` e o novo `:watch`), por uma opção do plugin de convenção, e
não em todos: as features e o `:shared` continuam sem watchOS. Nesses módulos `iosMain` vira `appleMain`, e o que é
só do iPhone (UIKit) fica em `iosMain` com o equivalente em `watchosMain`. Alvos: `watchosArm64` (arm64_32: Series 4 a
8 e SE), `watchosDeviceArm64` (Series 9 em diante e Ultra 2) e `watchosSimulatorArm64`.

### 4. O framework e o projeto

O `:watch` gera um framework próprio (`WatchShared`) pelo `embedAndSignAppleFrameworkForXcode`, sem CocoaPods (a 25.15
já prevê sair dele). O app do relógio é um target watchOS no `iosApp.xcodeproj`, embutido no app do iPhone e capaz de
rodar sem ele (`WKRunsIndependentlyOfCompanionApp`), em SwiftUI, com as mesmas pontes da ADR 0015 (`Watched`,
`@Observable`) e os mesmos tokens e textos gerados. watchOS mínimo 11 (o `@Observable` pede 10; o 11 acompanha o iOS
18 do app).

### 5. Áudio

`AVAudioSession` com `routeSharingPolicy: .longFormAudio`: no relógio, ativar a sessão mostra o seletor de saída e
toca em fones ou alto-falantes Bluetooth, o equivalente ao "só em fones" do Wear OS (19.9f). Isso pede um `actual` de
sessão de áudio para o watchOS no `:core:player`.

### 6. Sincronização

A mesma da 19.9b: o `SyncSnapshot` em JSON e o `planSync` sem mudança. O transporte é o WatchConnectivity
(`WCSession.transferFile`, que entrega mesmo com o outro lado fechado), em Swift dos dois lados, com 10 s de espera
como o `WearableSync`; o Kotlin só monta e aplica o snapshot.

## Consequências

- O 25.14 vira uma sequência de itens: 25.14a fundação (`appleMain`, alvos watchOS, `:watch`, o framework e a
  compilação no CI), 25.14b biblioteca, podcast e episódios, 25.14c player e áudio, 25.14d sincronização com o iPhone,
  25.14e downloads, 25.14f complicação e Smart Stack.
- O CI compila os módulos de dados para o watchOS, o que pega cedo uma dependência nova sem watchOS.
- Toda conferência visual depende do simulador do watchOS, que precisa ser baixado; não há Apple Watch para testar.
- Rever a decisão 2 se o Compose Resources passar a publicar watchOS: aí os ViewModels das features podem chegar ao
  relógio e as fachadas do `:watch` encolhem.
