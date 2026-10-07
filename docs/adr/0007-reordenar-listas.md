# ADR 0007 — Reordenar listas arrastando

- **Status:** aceita
- **Data:** 2026-10-06
- **Fase:** 24.4 do [roadmap](../ROADMAP_MELHORIAS.md); a fila da 18.6 usa a mesma base

## Contexto

A ordem personalizada da biblioteca (24.4) e a fila de reprodução (18.6) precisam de arrastar para reordenar, em lista
e em grade, nas quatro plataformas. O Compose não traz isso pronto: um `LazyColumn` ou `LazyVerticalGrid` não move
itens com o dedo, e fazer à mão pede detectar o arrasto, calcular sobre qual item ele está, rolar perto das bordas e
animar os vizinhos.

## Decisão

**Reorderable** (`sh.calvin.reorderable:reorderable` 3.1.0), que publica para Android, JVM, iOS e Wasm e funciona sobre
os `LazyListState`/`LazyGridState` que as telas já usam.

- O arrasto acontece numa tela própria, **"Organizar biblioteca"**, sempre em lista, aberta pelo menu de ordem; a
  biblioteca nunca mostra alças, nem na ordem personalizada nem na grade. A primeira versão punha a alça na própria
  biblioteca, inclusive sobre as capas da grade, e poluía a tela o tempo todo.
- Na tela, o arrasto é por uma **alça** (☰); o toque longo continua livre. Abrir a tela já passa a biblioteca para a
  ordem personalizada.
- Durante o arrasto, a tela mexe numa cópia local da lista; ao soltar, manda a ordem inteira ao ViewModel, que grava
  numa transação (`PodcastDao.reorder`), então sair da tela não perde nada. A lista que volta do banco substitui a
  cópia. Os itens são localizados pela chave, não pelo índice, porque a explicação no topo é um item da mesma lista.
- **Sem arrastar:** cada item ganha as ações de acessibilidade "mover para cima" e "mover para baixo" (TalkBack e
  VoiceOver), que mandam a mesma ordem.

Alternativa: implementar com `detectDragGesturesAfterLongPress` e `animateItem`; centenas de linhas para cobrir rolagem
automática, grade e acessibilidade, e de novo na 18.6.

## Consequências

- Nova dependência no `:feature:library` (e no da fila, quando a 18.6 chegar). A versão 3.1.0 foi compilada contra o
  Compose 1.7 e roda no 1.12 do projeto; ao atualizar o Compose, conferir o arrasto no Android e no iOS.
- O teclado do Desktop ainda não reordena (setas com um item focado); fica para quando a 21 tratar do Desktop.
- Se a biblioteca deixar de ser mantida, a troca fica restrita a `LibraryOrderScreen.kt` e à tela da fila.
