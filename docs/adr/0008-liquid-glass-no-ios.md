# ADR 0008 — Liquid Glass no iOS

- **Status:** aceita; a ser substituída pela ADR do item 25.1 (em 07/10/2026 o usuário decidiu fazer a interface do
  iOS toda em SwiftUI, na fase 25)
- **Data:** 2026-10-07
- **Fase:** 24.3 do [roadmap](../ROADMAP_MELHORIAS.md)

## Contexto

O iOS 26 trouxe o Liquid Glass: barras de abas e de ferramentas translúcidas que refratam o conteúdo por baixo. O
material é desenhado pelo sistema e só aparece em componentes nativos (`UITabBar`, `TabView`, `NavigationStack`,
toolbars). O Compose desenha os próprios pixels numa única view Metal, então nada do que ele desenha ganha o efeito.

Como o app está hoje no iOS:

- O Swift só embrulha o Compose: `ContentView` mostra um `UIViewControllerRepresentable` com o `MainViewController()`
  ([`ContentView.swift`](../../iosApp/iosApp/ContentView.swift)).
- Toda a navegação é do `RootComponent` (Decompose), em código comum: as duas abas da ADR 0005 com uma pilha cada,
  o player e "Organizar biblioteca" por cima de tudo, o estado salvo entre mortes do processo e o voltar. No iOS, o
  gesto da borda esquerda vem do `PredictiveBackGestureOverlay` em `MainViewController.kt`.
- A barra é o `NavigationSuiteScaffold` do Material 3: barra embaixo no celular e rail em tela larga (iPad, Desktop).
  O mini player fica logo acima dela, desenhado pelo `RootContent`.
- O app suporta a partir do iOS 16 (`IPHONEOS_DEPLOYMENT_TARGET`), então o iOS 16–18 continua sem Liquid Glass de
  qualquer jeito.

## Opções

### A · Casca SwiftUI com `TabView` e `NavigationStack` (tutorial da JetBrains)

O caminho do [tutorial oficial](https://kotlinlang.org/docs/multiplatform/ios-liquid-glass.html): o SwiftUI é dono das
abas, das pilhas e das barras de título, e cada tela é um `ComposeUIViewController` separado. Ganha tudo do sistema:
vidro nas abas e nas barras, a barra que encolhe ao rolar (`tabBarMinimizeBehavior`), o voltar nativo e até um lugar
para o mini player (`tabViewBottomAccessory`).

O custo é a navegação. As pilhas, o voltar e o estado salvo do `RootComponent` passariam a existir em dobro, um
coordenador em Swift espelhando o de Kotlin, só no iOS. O player e "Organizar biblioteca", que cobrem as abas, viram
apresentações nativas. Cada tela vira um `UIViewController` com o próprio Compose, então perde as animações entre
telas do Compose. E o iOS 16–25 continua precisando da navegação toda em Compose, ou seja, dois caminhos para manter.
Quem seguiu o tutorial num app real relata o mesmo problema
([DEV Community](https://dev.to/shivathapaa/i-built-jetbrains-official-liquid-glass-setup-then-deleted-it-4hoh)):
tudo o que a navegação carrega precisa ser refeito do lado Swift.

### B · `UITabBar` nativo por cima do Compose, só no iOS 26+

A navegação continua toda no `RootComponent`. No iOS 26+, o `RootContent` troca a barra do Material por um `UITabBar`
solto (sem `UITabBarController`), posto num `UIKitView` com `UIKitInteropProperties(placedAsOverlay = true)` por cima do
conteúdo. Assim o vidro refrata as telas em Compose que rolam por baixo. Para isso:

- um `expect`/`actual` "barra de abas da plataforma" com um parâmetro de aba selecionada e um callback, por um
  `CompositionLocal` ou por um slot no `RootContent`;
- no iOS, o `UITabBar` nas bindings do Kotlin/Native, sem código Swift, com um delegate que chama o
  `component.onTabClicked`;
- verificação de versão em tempo de execução: no iOS 16–25 fica a barra do Material de hoje;
- as telas passam a se estender por baixo da barra, com o espaço dela vindo pelos insets, como já acontece com o mini
  player (`LocalMiniPlayerInset`).

Fica de fora: a barra que encolhe ao rolar e o acessório inferior, que são do `TabView`. O mini player continua em
Compose, sem vidro, logo acima da barra. Em tela larga (iPad) continua o rail do Material, já que o `UITabBar` não tem
versão lateral. O Android não muda nada.

### C · Imitação no Compose (Haze ou bibliotecas de "liquid glass")

O [Haze](https://chrisbanes.github.io/haze/) desfoca o conteúdo por trás de um componente nas quatro plataformas, e há
bibliotecas KMP que imitam o Liquid Glass ([liquid-glass](https://klibs.io/project/NadeemIqbal/liquid-glass),
[KMPLiquidGlass](https://klibs.io/project/Kashif-E/KMPLiquidGlass), ainda em alfa). Daria um vidro igual nas quatro
plataformas, incluindo a barra e o mini player. Mas é uma aproximação: não refrata como o sistema, não acompanha os
ajustes de acessibilidade do iOS (reduzir transparência, aumentar contraste) nem as mudanças da Apple, e custa
desempenho em Android fraco. No Android ainda contraria o Material 3, que não usa vidro. E é mais uma dependência,
contra a ADR 0002.

### D · Não fazer nada

A barra do Material com a identidade "Sinal" (ADR 0001). Funciona igual em todas as versões, mas no iOS 26 o app
destoa dos apps do sistema, que passaram todos a ter a barra de vidro flutuante.

## Decisão

**B · `UITabBar` nativo por cima do Compose, só no iOS 26+**, como item próprio depois da 24.2, para os dois mexerem
na barra uma vez só.

- Dá o que mais aparece do Liquid Glass, a barra de abas, com o material real do sistema e seus ajustes de
  acessibilidade, sem duplicar a navegação: o `RootComponent`, o voltar, o player por cima e o estado salvo continuam
  em código comum.
- O código novo fica restrito a um `actual` no `iosMain` e a uma verificação de versão. Se der errado, apagar o
  `actual` volta à barra de hoje.
- A fica rejeitada porque o custo (navegação em dobro, só no iOS, mais um caminho para o iOS 16–25) é bem maior que o
  ganho em relação à B (barra que encolhe e acessório inferior). Vale reabrir se o Compose Multiplatform passar a
  oferecer uma ponte entre o `TabView` e uma navegação comum.
- C fica rejeitada para a barra pelos motivos acima. Um desfoque no mini player pode voltar na 24.2, como detalhe
  visual das quatro plataformas, se a referência visual pedir.

### Riscos a conferir no protótipo

1. O vidro refrata mesmo o conteúdo Metal do Compose por baixo de um `UIKitView` sobreposto, com a lista rolando, sem
   cair o quadro.
2. O toque passa direito: na barra vai para o `UITabBar`, fora dela para o Compose.
3. Os insets: o fim das listas, o "+" da biblioteca e as mensagens ficam acima da barra nativa, que tem outra altura.
4. O VoiceOver lê as abas uma vez só (da barra nativa) e o rótulo de cada aba vem das strings do `composeResources`.
5. O tema escuro e o claro seguem o do app, não só o do sistema.

Se o 1 ou o 2 falhar, a decisão volta para D até o Compose Multiplatform melhorar a sobreposição de views nativas.

## Consequências

- Novo item no roadmap, depois da 24.2: "Barra de abas nativa no iOS 26" com o protótipo e os cinco riscos acima.
- O `RootContent` passa a ter um ponto de troca da barra por plataforma. Hoje só o iOS usa; o Android e o Desktop
  ficam com o `NavigationSuiteScaffold`.
- No iOS 26 a barra deixa de seguir os tokens da ADR 0001 (fundo, aba ativa em âmbar) e passa a seguir o sistema. Dá
  para pôr a cor da aba ativa (`tintColor`) em âmbar, para manter a identidade.
