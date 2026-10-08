# ADR 0001 — Identidade visual: direção "Sinal"

- **Status:** aceita
- **Data:** 2026-10-02
- **Fase:** 9.1 do [roadmap](../ROADMAP_MELHORIAS.md)
- **Referência visual:** [`docs/podcast-design-system.html`](../podcast-design-system.html)

## Contexto

O app usava uma paleta monocromática (preto, branco e cinzas), a fonte do sistema e só 16 dos papéis de cor do
Material 3. Os demais papéis caíam no roxo padrão, que aparecia em chips, divisores e na navegação. Não havia
identidade de produto nem referência visual.

Comparamos três direções com os mesmos mockups nos dois temas e o contraste WCAG calculado:

| Direção | Cor | Fonte |
|---|---|---|
| **A · Sinal** | Âmbar | Onest |
| B · Estúdio | Azul cobalto | Plus Jakarta Sans |
| C · Ondas | Verde-petróleo | Figtree |

## Decisão

Adotamos a direção **A · Sinal**: âmbar de "estúdio no ar" sobre neutros levemente quentes, com a fonte **Onest**
embarcada em `composeResources/font` nos pesos 400, 500, 600 e 700 (licença OFL).

O âmbar puro (`#F2A93B`) tem só 1,9:1 contra o fundo claro e 2,0:1 contra o branco. Por isso ele tem três papéis:

| Papel | Claro | Escuro | Uso |
|---|---|---|---|
| `primary` | `#BC7300` | `#F2A93B` | Preenchimento de botões, progresso, aba ativa. No claro, 3,3:1 contra o fundo e 4,6:1 para o ícone escuro por cima |
| `accentText` | `#9A5B00` | `#F2A93B` | Âmbar como texto ou ícone pequeno ("Novo", "Tocando agora", nome do podcast no player) |
| `brand` | `#F2A93B` | `#F2A93B` | Selos, logo e destaques grandes, sempre com `onBrand` escuro por cima |

### Tokens de cor

Fonte da verdade para `ColorSchemes.kt` (os 48 papéis do M3, com os "fixed" iguais nos dois temas, como o M3
define) e `PodcastColors.kt` (os 5 últimos, fora do M3). Os valores ficam como primitivas com nome em
`Palette.kt` (`AMBER_50`, `NEUTRAL_97`…, o número é o tom de 0 a 100), e os papéis apontam para elas.

| Token | Claro | Escuro |
|---|---|---|
| `background` | `#F7F7F5` | `#161513` |
| `surface` | `#F7F7F5` | `#161513` |
| `surfaceContainerLowest` | `#FFFFFF` | `#110F0E` |
| `surfaceContainerLow` | `#F2F1EE` | `#1D1B19` |
| `surfaceContainer` | `#ECEBE7` | `#22201D` |
| `surfaceContainerHigh` | `#E6E4DF` | `#2C2A26` |
| `surfaceContainerHighest` | `#E0DDD7` | `#37342F` |
| `onSurface` | `#1C1B19` | `#F1EFEA` |
| `onBackground` | `#1C1B19` | `#F1EFEA` |
| `surfaceVariant` | `#E0DDD7` | `#37342F` |
| `surfaceTint` | `#BC7300` | `#F2A93B` |
| `surfaceBright` | `#F7F7F5` | `#3C3934` |
| `surfaceDim` | `#DCD9D3` | `#161513` |
| `onSurfaceVariant` | `#605C55` | `#A8A398` |
| `outline` | `#8A857C` | `#7D776E` |
| `outlineVariant` | `#D6D2CA` | `#3A3631` |
| `primary` | `#BC7300` | `#F2A93B` |
| `onPrimary` | `#1C1B19` | `#1C1B19` |
| `primaryContainer` | `#FCE3BC` | `#5A3600` |
| `onPrimaryContainer` | `#4F2F00` | `#FCE3BC` |
| `secondary` | `#6E6152` | `#D6C6B3` |
| `onSecondary` | `#FFFFFF` | `#2A2118` |
| `secondaryContainer` | `#EFE4D6` | `#4A3F33` |
| `onSecondaryContainer` | `#2A2118` | `#EFE4D6` |
| `tertiary` | `#3F6872` | `#A9CCD6` |
| `onTertiary` | `#FFFFFF` | `#0E2A31` |
| `tertiaryContainer` | `#D4E6EB` | `#2E4C55` |
| `onTertiaryContainer` | `#0E2A31` | `#D4E6EB` |
| `error` | `#B3261E` | `#FFB4AB` |
| `onError` | `#FFFFFF` | `#690005` |
| `errorContainer` | `#F9DEDC` | `#93000A` |
| `onErrorContainer` | `#410E0B` | `#FFDAD6` |
| `inverseSurface` | `#31302D` | `#F1EFEA` |
| `inverseOnSurface` | `#F4F2EE` | `#31302D` |
| `inversePrimary` | `#F2A93B` | `#9A5B00` |
| `scrim` | `#000000` | `#000000` |
| `primaryFixed` / `primaryFixedDim` | `#FCE3BC` / `#F2A93B` | iguais ao claro |
| `onPrimaryFixed` / `onPrimaryFixedVariant` | `#2B1900` / `#5A3600` | iguais ao claro |
| `secondaryFixed` / `secondaryFixedDim` | `#EFE4D6` / `#D6C6B3` | iguais ao claro |
| `onSecondaryFixed` / `onSecondaryFixedVariant` | `#2A2118` / `#4A3F33` | iguais ao claro |
| `tertiaryFixed` / `tertiaryFixedDim` | `#D4E6EB` / `#A9CCD6` | iguais ao claro |
| `onTertiaryFixed` / `onTertiaryFixedVariant` | `#0E2A31` / `#2E4C55` | iguais ao claro |
| `accentText` | `#9A5B00` | `#F2A93B` |
| `brand` | `#F2A93B` | `#F2A93B` |
| `onBrand` | `#1C1B19` | `#1C1B19` |
| `downloaded` | `#2B7349` | `#7FD6A0` |
| `played` | `#8A857C` | `#7D776E` |

Os 26 pares de uso (texto sobre fundo, ícone sobre botão, botão contra fundo, borda contra fundo…) passam no AA
nos dois temas: 4,5:1 para texto e 3:1 para ícones, bordas e formas.

### Demais tokens

- **Tipografia:** `headlineMedium` 28/34 700 · `titleLarge` 22/28 600 · `titleMedium` 16/22 600 · `bodyLarge`
  16/24 400 · `bodyMedium` 14/20 400 · `labelLarge` 14/20 600 · `labelMedium` 12/16 600 · `section` 11/16 600 em
  caixa alta com +0,06em · `timer` 13/18 500 com algarismos tabulares.
- **Espaçamento:** `Spacing` 2 · 4 · 8 · 12 · 16 · 24 · 32 · 48 dp (`xxs` a `xxxl`).
- **Formas:** 6 · 10 · 14 · 20 · 28 dp (`extraSmall` a `extraLarge`).
- **Tamanhos:** `touchTarget` 48 · `artworkS` 56 · `artworkM` 140 · `artworkL` 280 · `miniPlayerHeight` 64 ·
  `playButtonLarge` 72 dp.
- **Movimento:** 150 · 250 · 400 ms; curvas `standard` (0,2, 0, 0, 1) e `emphasized` (0,05, 0,7, 0,1, 1), esta só
  para elementos que mudam de lugar.
- **Opacidade:** `disabled` 0,38 · `muted` 0,60 · `scrim` 0,32 · `faint` 0,12 (trilhas e tons sobre contêiner colorido).

### Ícone

Opção **B · No ar** (escolhida entre três): microfone com duas ondas de cada lado, em tinta escura (`#1C1B19`)
sobre o âmbar da marca (`#F2A93B`), 8,6:1. Fontes vetoriais em [`docs/brand/`](../brand/): `icon.svg` (padrão),
`icon-dark.svg` (glifo âmbar sobre `#1C1B19`), `icon-tinted.svg` (glifo branco sobre preto, que o iOS tinge) e
`favicon.svg`. O glifo ocupa 75% do quadro de 108 para as ondas ficarem dentro da zona segura de 66 do ícone
adaptativo do Android. `docs/brand/generate-icons.sh` regenera os PNG, `.icns` e `.ico`; o Android usa vetores.

### Layout das telas (24.2)

Revisão aceita em 2026-10-07, desenhada na seção "Telas" da referência visual, sobre a navegação da ADR 0005:

- **Hierarquia:** títulos de seção ("Continuar ouvindo", "Podcasts", datas) e título grande que encolhe ao rolar.
- **Cabeçalho de capa:** o detalhe do podcast e a tela do episódio abrem com a capa centrada, maior, sobre o fundo
  tingido pela cor dela (`ArtworkBackdrop`, o mesmo do player).
- **Biblioteca:** faixa "Continuar ouvindo" com os episódios começados no topo, só quando há algum.
- **Episódios:** lista dividida por data, com o cabeçalho preso ao rolar.
- **Player:** capa na largura toda; o play é um quadrado de cantos grandes tocando e um círculo pausado; o progresso
  é ondulado tocando e reto pausado; velocidade, timer e fila em pílulas.
- **Capa compartilhada:** a capa do mini player cresce até a do player (`SharedTransitionLayout`, `Motion.long`
  enfatizada).
- **Material 3 Expressive:** os componentes prontos só existem em versões alfa do Material 3 (a 1.9.0 estável do
  Compose Multiplatform traz só os tokens). Pela ADR 0002 ficamos na estável e fazemos à mão só o botão que muda de
  forma e o progresso ondulado; trocar pelos oficiais quando saírem em versão estável.

## Consequências

- No tema claro, um botão `primary` só pode ficar sobre `background`, `surfaceContainerLowest` ou `surfaceContainer`.
  Sobre `surfaceContainerHigh` ou mais alto, o contraste cai abaixo de 3:1.
- `primary` nunca é usado como cor de texto; para âmbar em texto existe `accentText`.
- Um teste (`ColorContrastTest`, item 9.3) recalcula os 26 pares a partir dos tokens em Kotlin e falha abaixo de AA,
  então mudar uma cor exige mudar esta tabela, a página de referência e o código juntos.
- Perto do laranja do Overcast: o âmbar fica mais amarelo e os neutros mais quentes, para manter distância.
- A cor dinâmica pela capa (9.10) precisa de fallback para `primary` quando a cor extraída não atingir o contraste.
