# ADR 0004 — Leitor XML dos feeds

- **Status:** aceita
- **Data:** 2026-10-06
- **Fase:** 15.1

## Contexto
O `RssXmlParser` procurava tags com `indexOf` sobre o texto. Os fixtures reais da 15.2 mostraram o efeito: `&amp;`
ficava codificado nos títulos e nas URLs de áudio (NPR e The Daily: o áudio não tocava), um `<item id="…">` sumia, e
`language`/`link` vinham do primeiro que aparecesse no documento, inclusive dentro de um item. Tipo do enclosure,
`explicit`, temporada, número do episódio e categorias eram fixos ou vazios.

## Decisão
Usar o leitor de baixo nível do **xmlutil** (`io.github.pdvrieze.xmlutil:core`, `KtXmlReader`), sem a parte de
serialização. Ele é Kotlin puro e publica para todos os alvos do projeto (JVM, Android, iOS, Wasm), e também para
watchOS (19.9c). O parser monta uma árvore pequena e lê os campos por nome; os prefixos `itunes:` e `content:` vêm do
namespace, então um feed que use outro prefixo continua funcionando.

O leitor roda em modo `relaxed`: aceita prefixos não declarados e XML quebrado ou truncado, lendo o que conseguir, como
o parser antigo. Entidades HTML que o XML não declara (`&nbsp;`) ficam escritas como vieram.

Alternativas:
- **Corrigir o parser à mão:** sem dependência, mas cada caso novo (entidades numéricas, atributos, CDATA no meio do
  texto, namespaces) seria mais um remendo, e o formato do XML continuaria sem garantia.
- **`kotlinx-serialization` com xmlutil:** mapeia o XML direto para classes, mas RSS tem muitas extensões opcionais e
  repetidas, e o mapeamento ficaria maior que a leitura por nome.

## Consequências
- Nova dependência no `:data`. Atualizar junto com o Kotlin; se uma versão deixar de publicar um alvo, a 15.1 volta a
  ser decidida aqui.
- O documento inteiro vira árvore antes de ser lido. Feeds de milhares de episódios (o The Daily tem 20 MB) ocupam
  mais memória durante a leitura; se a 23 medir isso como problema, trocar a árvore por leitura em fluxo dos itens.
- `RealFeedFixturesTest` e `RssXmlParserTest` são a referência: qualquer troca de leitor tem que passar neles.
