# Revisão de UX tela a tela (24.1)

Feita em 06/10/2026 com o código da fase 15 (`feature/phase-24-navigation-ux`), percorrendo as telas no **Razr 60**
(Android 16, tema escuro, 12 feeds reais), no **simulador do iPhone 17** (iOS 26.5, tema claro, 11 feeds) e no
**Desktop** (macOS, janela padrão de 800×600, um feed). Cada achado diz onde aparece e para qual item vai.

Não coberto: estados sem rede (desligar a rede derrubaria o adb sem fio do Razr), TalkBack/VoiceOver (a 9.12 já
cuidou dos rótulos; uma passada com leitor de tela entra na 17.4) e a Web.

## Navegação

| # | Achado | Onde | Vai para |
|---|---|---|---|
| N1 | Deslizar da borda esquerda não volta. A navegação registra o `backHandler` do Decompose, mas o iOS não liga o gesto de borda a ele; só a seta da barra volta | iOS | 24.8 |
| N2 | O Player é aba e tela cheia ao mesmo tempo: como aba, mostra a seta de recolher e "Agora ouvindo"; vazio, mostra todos os controles como se estivessem ativos, "-0:00" e nenhuma chamada para ação | todas | 24.7 |
| N3 | "Buscar" lista e procura episódios já salvos | todas | 24.6 |
| N4 | Downloads vazio repete o filtro "Baixados" do detalhe | todas | 24.5 |

## Biblioteca

| # | Achado | Onde | Vai para |
|---|---|---|---|
| B1 | Nenhum card mostra o contador de não ouvidos, embora todos os episódios estejam sem ouvir (o `PodcastCard` aceita o número, ninguém passa) | todas | 24.4 |
| B2 | Só grade, sem ordenação | todas | 24.4 |
| B3 | Excluir o podcast só existe no toque longo do card, sem nenhuma pista na tela; no Desktop, o clique direito não abre nada | todas | 24.9 |
| B4 | Estado vazio com dois botões para a mesma ação (o "+" flutuante e "Adicionar Podcast") e o texto "Toque em +…", que no Desktop é clique | todas | 24.12 |
| B5 | O "+" flutuante cobre parte do último card ao rolar até o fim | Android, iOS | 24.12 |

## Adicionar podcast

| # | Achado | Onde | Vai para |
|---|---|---|---|
| A1 | O campo não recebe o foco ao abrir; o que se digita se perde até clicar nele | Desktop | 24.13 |
| A2 | Enter não envia; não há botão de colar, e o caminho comum é colar um link | todas | 24.13 |
| A3 | Rótulo "Insira a URL do feed RSS:" com dois-pontos e jargão ("feed RSS") | todas | 24.13 |

## Detalhe do podcast

| # | Achado | Onde | Vai para |
|---|---|---|---|
| P1 | A barra diz só "Podcast"; o nome aparece abaixo e some ao rolar, e a barra não o assume | todas | 24.12 |
| P2 | A descrição inteira fica acima dos episódios (no IA Sob Controle, 8 linhas no Razr), sem "ver mais" | todas | 24.12 |
| P3 | "Marcar como ouvido" (este ou este e os anteriores) só no toque longo da linha, e o diálogo não tem "cancelar" nem "marcar como não ouvido" | todas | 24.9, 18.10 |
| P4 | Sem ordenação nem temporadas | todas | 18.10 |

## Episódio

| # | Achado | Onde | Vai para |
|---|---|---|---|
| E1 | "Reproduzir" não muda: não diz "Continuar · 44 min restantes" num episódio começado, nem vira "Pausar" quando ele toca | todas | 24.14 |
| E2 | A tela não tem baixar, adicionar à fila, data nem duração; para isso é preciso voltar à lista | todas | 24.14 |

## Player

| # | Achado | Onde | Vai para |
|---|---|---|---|
| R1 | Os rótulos de velocidade, fila e timer ficam atrás da barra de abas; numa janela de 600 px de altura, os botões de baixo ficam cortados, e a tela não rola | iOS, Desktop | 24.10 |
| R2 | Antes de carregar o episódio, a barra de progresso aparece cheia, com "0:22" e "-0:00": a duração desconhecida vira zero | iOS, Desktop | 24.11 |
| R3 | Velocidade, fila e timer são `AlertDialog`; a fila mostra só títulos | todas | 24.2, 18.6 |

## Listas de episódios

| # | Achado | Onde | Vai para |
|---|---|---|---|
| L1 | Duração em formatos diferentes na mesma lista: "2h 51min" para o total e "172 min restante" para o que falta (cortado em "172 mi…") | todas | 24.12 |
| L2 | Títulos de duas linhas cortados com reticências e o nome do podcast cortado na busca; com download e play à direita, sobra pouco espaço no celular | Android, iOS | 24.2 |

## O que está bom

Estados vazios com ícone, título e explicação; destaque de linha ao passar o mouse no Desktop; tema escuro e claro
coerentes; descrição com links, listas e entidades (15.7); mini player que some ao rolar e volta; snackbar com a causa
dos erros (URL inválida, já na biblioteca).
