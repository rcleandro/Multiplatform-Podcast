# ADR 0005 — Navegação

- **Status:** aceita
- **Data:** 2026-10-06
- **Fase:** 24.5, 24.6 e 24.7 do [roadmap](../ROADMAP_MELHORIAS.md), com o lugar das telas da fase 18

## Contexto

A barra tem quatro abas: Biblioteca, Buscar, Downloads e Player. A revisão de UX ([24.1](../revisao-ux-24.1.md))
mostrou que três delas repetem outro caminho ou não dizem o que mostram:

- **Buscar** lista e procura episódios já salvos, não podcasts. A 18.1 planejava pôr a busca de podcasts novos nessa
  mesma aba, misturando duas coisas diferentes.
- **Downloads** repete o filtro "Baixados" que o detalhe do podcast já tem.
- **Player** também abre pelo mini player e, como aba, fica vazio quando nada toca, com todos os controles como se
  estivessem ativos e uma seta de "recolher" que não faz sentido numa aba.

A fase 18 traz quatro telas que ainda não têm lugar: busca de podcasts (18.1), Configurações (18.5), fila (18.6) e
novos episódios (18.7).

## Decisão

Duas abas: **Biblioteca** e **Episódios**. Tudo o mais abre a partir delas.

```
Biblioteca ──┬─ "+" ── Adicionar podcast: busca no diretório (18.1) · menu: adicionar por URL, importar OPML (18.13)
             ├─ ⚙  ── Configurações (18.5), com o espaço ocupado pelos downloads (14.8)
             ├─ menu de ordem › "Organizar…" ── Organizar biblioteca (24.4), sobre as abas
             └─ card ─ Detalhe do podcast ── Episódio

Episódios ── busca nos episódios salvos
             filtros: Novos (18.7, padrão) · Em andamento · Baixados · Todos
             └─ linha ─ Episódio

Mini player (em todas as telas, com algo tocando) ── Player em tela cheia, sem barra de abas ── Fila (18.6)
```

- **Downloads deixa de ser aba (24.5):** vira o filtro "Baixados" de Episódios. O espaço ocupado aparece no topo desse
  filtro e nas Configurações. Sem rede, o app abre em Episódios › Baixados, que é o que dá para ouvir.
- **Player deixa de ser aba (24.7):** abre pelo mini player e pela notificação, em tela cheia e sem barra de abas, e
  a seta de recolher volta a fazer sentido. Sem nada tocando, não há player para abrir. Nas telas largas (21.1), o
  rail fica com as mesmas duas entradas.
- **"Buscar" vira "Episódios" (24.6):** nome, ícone e textos nos três idiomas. A busca de podcasts novos (18.1) fica
  na tela "Adicionar podcast", aberta pelo "+" da biblioteca, junto de "Adicionar por URL", que é o diálogo de hoje.
- **Novos episódios (18.7)** é o filtro padrão de Episódios, não uma aba.
- **Configurações (18.5)** abre por um ícone na barra superior da biblioteca.
- **Fila (18.6)** abre pelo player e pelo mini player.
- **Organizar biblioteca (24.4)** abre pelo item "Organizar…" do menu de ordem, sobre as abas, como o player; voltar
  ou tocar numa aba fecha. É o único lugar que mostra as alças de arrastar (ADR 0007).

Alternativas consideradas:

- **Manter Downloads e Player como abas:** quatro abas, duas delas repetindo outro caminho.
- **Aba "Descobrir" para a busca de podcasts:** uma aba para algo que se usa ao montar a biblioteca e raramente
  depois.
- **Busca de podcasts e de episódios na mesma aba** (o plano antigo da 18.1): duas listas de coisas diferentes na
  mesma tela, com o mesmo campo.
- **Aba "Novos" separada:** três abas mais perto do Apple Podcasts, mas Novos e Episódios mostrariam a mesma lista
  com filtros diferentes.

## Consequências

- **Duas abas** é menos que as 3 a 5 que o Material indica para a barra de navegação. Fica assim enquanto não houver
  um terceiro destino que se justifique; se a 19 trouxer um (por exemplo, filas inteligentes, 19.4), ele entra aqui.
- O app passa a lembrar e abrir o filtro de Episódios conforme a rede, então a rede vira parte do estado da tela.
- O player deixa de ter entrada própria na barra: o mini player passa a ser o único caminho na tela, então precisa
  estar em todas as telas com algo tocando (ele continua sumindo ao rolar e voltando ao parar).
- Sai o `DownloadedEpisodesScreen` como destino; sua lista vira o filtro, e o texto do espaço ocupado muda de lugar.
- Revisar se Episódios › Novos cumpre o papel da antiga aba Buscar com dados de uso (16) depois de publicado.
- **Implementação (06/10/2026):** as duas abas e os filtros Todos, Em andamento e Baixados estão no app. Faltam o filtro
  Novos, que depende da 18.7 (guardar a última visita), e abrir em Baixados sem rede (24.15), que depende de detectar
  a rede; até lá, Episódios abre em Todos.
