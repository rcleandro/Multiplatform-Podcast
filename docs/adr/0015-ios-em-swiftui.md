# ADR 0015 — Interface do iOS em SwiftUI

- **Status:** aceita (decisão do usuário em 24/10/2026, as seis recomendações)
- **Data:** 2026-10-24
- **Fase:** 25.1 do [roadmap](../ROADMAP_MELHORIAS.md); substitui a [ADR 0008](0008-liquid-glass-no-ios.md)

## Contexto

Na fase 25, a interface do iOS passa a ser toda em SwiftUI, sobre o Kotlin que já é compartilhado. Hoje o Swift só
embrulha o Compose: `ContentView` mostra o `MainViewController()`
([`MainViewController.kt`](../../shared/src/iosMain/kotlin/br/com/carvalho/podcast/MainViewController.kt)), que inicia
o Koin e monta o `RootComponent`. O que o Swift vai consumir:

- **11 ViewModels** (`androidx.lifecycle.ViewModel`, registrados no Koin com `viewModelOf`). Todos seguem o mesmo
  formato: um `StateFlow` de estado (`uiState`), um `Flow<UiMessage>` de mensagens (um `Channel`) e
  `onIntent(intent)` com um `sealed interface` de intents. Dois (`SearchViewModel` e `PodcastDetailViewModel`)
  expõem `Flow<PagingData<Episode>>`. Alguns têm fluxos de efeito próprios (`DiscoverViewModel.openPodcast`).
- **A navegação** é uma máquina de estados simples, e não uma `childStack`: o `RootComponent` expõe um
  `Value<NavigationState>` serializável (aba, pilha de `Detail` por aba, flags do que fica por cima: player, fila,
  transcrição, organizar, adicionar, Configurações, estatísticas) e uma função por gesto (`onTabClicked`,
  `onPodcastSelected`, `onBackClicked`…), coberta pelo `RootComponentTest`. No iOS, hoje, ela não é restaurada depois
  que o sistema mata o processo: o `DefaultComponentContext` é criado sem `stateKeeper`.
- **Os textos** são 432 strings e 11 plurais em 15 idiomas, nos `composeResources` de `:core:ui` e
  `:core:designsystem`. Os ViewModels emitem `StringResource` (regra do `CLAUDE.md`). O Swift não lê esses arquivos.
  No iOS, o idioma do app já é o do sistema (o ajuste por app nos Ajustes do iOS): `ProvideAppLanguage` não faz nada
  lá.
- **A identidade visual** (ADR 0001) mora em Kotlin: as cores dos dois temas, a escala de tipos, os espaçamentos, e
  a Onest em quatro pesos (`core/designsystem/.../composeResources/font`).
- **Versões:** Kotlin 2.4.21, Compose Multiplatform 1.12.2. O app declara iOS 16.0 (`IPHONEOS_DEPLOYMENT_TARGET`,
  `Podfile`), mas o podspec do framework declara 18.2 (`ios.deploymentTarget` em `shared/build.gradle.kts`, desde
  `f63370d7`).
- **Pontes disponíveis em 24/10/2026:** SKIE 0.10.15 (25/09) suporta o Kotlin 2.4.20, e a 2.4.21 não aparece na
  lista; KMP-NativeCoroutines 1.0.6 (07/09) também é para a 2.4.20; o Swift export do Kotlin continua alpha na 2.4,
  com `Flow` exportado como `AsyncSequence` só por chamada explícita.

## Decisões

### 1. Como o Swift consome os ViewModels

| Opção | A favor | Contra |
|---|---|---|
| A · SKIE | `Flow` vira `AsyncSequence`, `suspend` vira `async`, `sealed` vira `enum` com `switch` exaustivo, argumentos padrão | Plugin do compilador preso à versão do Kotlin: cada atualização do Kotlin espera o SKIE (hoje a 2.4.21 não está listada) |
| B · KMP-NativeCoroutines | Parecido com A para `Flow` e `suspend` | Também preso ao Kotlin (KSP); pede anotação em cada propriedade exposta |
| C · Adaptadores à mão | Sem dependência nova; o formato dos 11 ViewModels é o mesmo, então um adaptador serve para todos | Intents montadas como classes (`LibraryIntentRefresh()`), sem `switch` exaustivo; o adaptador do `PagingData` é nosso |
| D · Swift export | O caminho oficial da JetBrains | Alpha, com mudanças incompatíveis anunciadas |

**Recomendação: C.**
- O formato único dos ViewModels torna o adaptador pequeno. No Kotlin (`iosMain`), uma função
  `Flow<T>.watch(onEach): Cancellable` para o estado, as mensagens e os efeitos. No Swift, uma classe `@Observable`
  genérica que guarda o ViewModel, publica o estado, repassa `onIntent` e, no `deinit`, cancela a observação e
  limpa o `ViewModelStore` do ViewModel (o mesmo ciclo de vida da 23.10).
- O `PagingData` passa por um `PagingDataPresenter` (paging-common) no `iosMain`, exposto como lista atual mais
  `loadAround(index)`.
- A e B travariam as atualizações do Kotlin, que o Dependabot traz toda semana, contra a ADR 0002. Reabrir quando o
  Swift export sair do alpha.

### 2. Quem é dono da navegação

| Opção | A favor | Contra |
|---|---|---|
| A · `RootComponent` em Kotlin, desenhado pelo Swift | Uma regra só nas quatro plataformas (aba que volta à raiz, link de podcast, início offline, escopo dos ViewModels da 23.10); o `RootComponentTest` continua valendo | O Swift sincroniza o `path` da `NavigationStack` e as folhas com o estado |
| B · Navegação em Swift | O jeito mais natural do SwiftUI | Duplica as regras do `RootComponent` só no iOS, sem os testes |

**Recomendação: A.** O `TabView` liga a aba ao `selectedTab` (trocar chama `onTabClicked`). Cada aba tem uma
`NavigationStack` com `path` derivado da pilha; o gesto de voltar encurta o `path`, que vira `onBackClicked`.
Player, fila e transcrição são `fullScreenCover`/`sheet` ligados às flags. Na mesma mudança, o iOS ganha o que falta
hoje: o `NavigationState` salvo em `@SceneStorage` (JSON do `stateKeeper`) e restaurado ao reabrir, como no Android.

### 3. De onde vêm os textos

| Opção | A favor | Contra |
|---|---|---|
| A · `Localizable.xcstrings` gerado dos `composeResources` | Nativo: plurais do sistema, idioma por app dos Ajustes do iOS, prévias do Xcode; serve também aos widgets, ao CarPlay e às App Intents (25.12, 25.13), que não rodam o Kotlin | Um gerador: converter `%1$s`/`%1$d` para `%@`/`%lld` e os plurais |
| B · Strings resolvidas pelo Kotlin (`getString`) | Sem gerador | Toda tela espera um `suspend` para cada texto; as extensões ficam sem textos |

**Recomendação: A.** A fonte continua sendo os `composeResources` (regra do `CLAUDE.md`). Uma tarefa do Gradle gera
o `iosApp/iosApp/Localizable.xcstrings`, que fica no repositório, e outra, no CI, falha se ele estiver
desatualizado, como o `checkModuleDependencies`. Os ViewModels continuam emitindo `StringResource`; o Swift usa a
chave (`StringResource.key`) com `String(localized:)`.

### 4. Tokens da ADR 0001

**Recomendação:** gerados do mesmo Kotlin que o Compose usa, não copiados à mão. Uma tarefa JVM do
`:core:designsystem` lê os esquemas claro e escuro e escreve um *color set* por cor no `Assets.xcassets`, com as
duas aparências, e um `DesignTokens.swift` com espaçamentos, raios e a escala de tipos. O resultado fica no
repositório, com a mesma checagem de desatualizado no CI. As quatro TTF da Onest são copiadas dos `composeResources`
para o app (`UIAppFonts`). A escala usa `Font.custom(_:size:relativeTo:)`, que segue o Dynamic Type.

### 5. Versão mínima do iOS

| Opção | Aparelhos | Ganho |
|---|---|---|
| 16 (hoje) | iPhone 8 e X em diante | Nenhum; o framework já declara 18.2 |
| 17 | XS/XR em diante | `@Observable`, `ContentUnavailableView` |
| 18 | XS/XR em diante, os mesmos do 17 | Também a API `Tab` do `TabView`, a transição `zoom`, `onScrollGeometryChange` e `@Entry` |

**Recomendação: iOS 18,** no app, no `Podfile` e no podspec. O iOS 17 e o 18 rodam nos mesmos iPhones, então o 18
não custa nenhum aparelho a mais que o 17, e corrige a diferença de hoje entre o app e o framework. O Liquid Glass e
o `tabViewBottomAccessory` continuam atrás de `#available(iOS 26, *)` (25.3).

### 6. O Compose sai do iOS

**Recomendação: sim, na 25.8.** As telas em Compose de cada `:feature`, do `:core:ui` e do `:core:designsystem`
passam de `commonMain` para um source set intermediário (`composeMain`) usado pelo Android, pelo Desktop e pela Web,
e não pelo iOS. Os ViewModels ficam em `commonMain`. Sem isso, o app iOS levaria o Compose inteiro sem desenhar nada
com ele. Medir o framework de release antes e depois. Os `StringResource` mantêm no iOS só o runtime do Compose
(pequeno, sem UI). Trocá-los por um tipo próprio só se a medição mostrar que vale.

## Riscos a conferir na 25.2

1. A observação de `StateFlow` pelo adaptador não vaza memória: o ViewModel é limpo ao fechar a tela (contagem de
   instâncias no Instruments, como o `ScopedViewModelsTest` faz no Kotlin).
2. A sincronização do `path` com o `RootComponent` não gera um laço (estado → `path` → `onBackClicked` → estado),
   inclusive no gesto de voltar interrompido no meio.
3. O `Localizable.xcstrings` gerado passa no `xcstringstool`, e os 15 idiomas aparecem nos Ajustes do iOS.
4. O app sobe no iOS 18 mais antigo disponível no simulador e no iOS 26.

## Consequências

- A ADR 0008 fica substituída por esta, e o `UITabBar` sobre o Compose não é feito.
- Cada tela passa a existir duas vezes, em Compose e em SwiftUI. Uma mudança de tela vale nas duas, e a lista de
  tamanhos de tela do `CLAUDE.md` vale também para o SwiftUI.
- O CI ganha duas checagens de arquivo gerado (textos, tokens) e um teste de UI em XCTest (25.2).
- Rever a decisão 1 quando o Swift export sair do alpha, e a 6 se o tamanho medido não justificar a separação.
