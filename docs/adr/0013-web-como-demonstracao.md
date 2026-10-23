# ADR 0013 — A Web como demonstração

- **Status:** aceita
- **Data:** 2026-10-23
- **Fase:** 20.4 do [roadmap](../ROADMAP_MELHORIAS.md)

## Contexto

Na Web, o feed é baixado pelo próprio navegador (`fetch`, pelo motor `ktor-client-js`). O navegador só entrega a
resposta de outro domínio se o servidor mandar `Access-Control-Allow-Origin`, e a maioria das hospedagens de podcast
não manda. Esses feeds não abrem na Web. A página não tem como contornar o CORS: o bloqueio é do navegador, e ele nem
diz à página que o motivo foi esse. Uma recusa por CORS e uma conexão caída chegam do mesmo jeito, como
`Error("Fail to fetch")` do Ktor.

A Web já não tem downloads nem Firebase (14.7).

## Decisão

**A Web fica como demonstração**, sem servidor próprio (decisão do usuário em 23/10/2026):

- Um feed que o navegador recusa vira `AppError.BrowserBlocked`, e o app explica: o navegador não conseguiu ler o
  feed, muitos sites de podcast só deixam apps lerem, use o app no celular ou no computador, ou confira a conexão. A
  mensagem cobre os dois casos porque o navegador não separa um do outro.
- Os feeds que mandam o cabeçalho (e a busca no diretório da Apple, que manda) continuam funcionando.
- `BrowserFetchFailureTest` (`:core:network`, roda no Chrome pelo `wasmJsBrowserTest` e no CI) confere que o Ktor
  ainda lança esse `Error` com essa mensagem; se uma atualização mudar, o teste quebra antes de a explicação sumir.

Alternativa:
- **Proxy mínimo** (um Cloudflare Worker que só repassa feeds): a Web passaria a ler qualquer feed, mas seria um
  serviço a mais para manter e pagar, e as URLs dos feeds, inclusive as privadas com token, passariam por ele.

## Consequências

- **Hospedagem:** o servidor da Web precisa mandar `Cross-Origin-Opener-Policy: same-origin` e
  `Cross-Origin-Embedder-Policy: credentialless` (ou `require-corp`), como o servidor de desenvolvimento já faz
  (`webApp/webpack.config.d/headers.js`). Sem eles o SQLite não usa o OPFS e o banco some a cada recarga (10.10).
- Revisar se a Web virar um produto e não só uma vitrine: aí o proxy volta à mesa, com uma ADR sobre o que ele vê.
