# ADR 0009 — Feeds e áudio em `http://`

- **Status:** aceita
- **Data:** 2026-10-08
- **Fase:** 16.7 do [roadmap](../ROADMAP_MELHORIAS.md)

## Contexto

Ainda há feeds, áudios e capas servidos só em `http://`, sobretudo de podcasts antigos e de hospedagens próprias. O
Android bloqueia texto claro por padrão desde a API 28, e o iOS faz o mesmo pelo App Transport Security (ATS). O app
não tinha exceção em nenhum dos dois, então esses podcasts não eram adicionados e os episódios não tocavam. O Desktop
aceitava `http`. A Web, servida em `https`, bloqueia conteúdo misto, e o navegador não deixa a página mudar isso.

Um feed em `http` não traz o token de um feed privado (esses são servidos em `https`), mas o que trafega em texto
claro pode ser lido e alterado no caminho.

## Decisão

**Tentar `https` primeiro e usar `http` só se falhar** (decisão do usuário em 08/10/2026):

- O plugin `HttpsFirst`, no cliente comum (`podcastDefaults`), troca `http://` por `https://` antes do pedido e, se a
  conexão ou o TLS falhar, refaz o pedido em `http`. Endereços com porta explícita ficam como estão. Ele roda dentro
  de cada tentativa do `HttpRequestRetry`, para a queda para `http` ser imediata. Vale para feeds e downloads.
- O texto claro foi liberado para o que não passa pelo cliente: o player toca a URL guardada e as capas vêm do cliente
  do Coil. No Android, `android:usesCleartextTraffic="true"`; no iOS, `NSAllowsArbitraryLoads` no `Info.plist`.
- A Web continua sem `http`.

Alternativas:
- **Só `https`, com uma mensagem de erro clara:** sem exceção no ATS, mas deixa de fora os podcasts que só existem em
  `http`.
- **Texto claro só para o áudio** (`NSAllowsArbitraryLoadsForMedia`): cobre o AVPlayer, mas não os downloads nem os
  feeds no iOS.

## Consequências

- A Apple pede uma justificativa para o `NSAllowsArbitraryLoads` na revisão. A justificativa: um agregador de
  podcasts abre feeds e áudios de qualquer servidor, escolhidos pelo usuário.
- Um host com a porta 443 filtrada atrasa o pedido até o timeout de conexão (15 s) antes de cair para `http`.
- A queda para `http` só acontece por falha de conexão ou de TLS. Um host que responda 404 em `https` e 200 em `http`
  continua falhando; se isso aparecer, a queda passa a considerar também o status.
- O player e as capas não tentam `https` antes: usam a URL do feed como veio.
