# Architecture Decision Records

One file per decision, numbered in order: `NNNN-titulo-curto.md`. A decision that replaces another sets the old
one to "substituída por ADR NNNN" instead of deleting it.

| ADR | Decisão |
|---|---|
| [0001](0001-identidade-visual.md) | Identidade visual "Sinal": cores, tipografia, tokens e ícone |
| [0002](0002-dependencias-instaveis-e-remendos.md) | Dependências instáveis e remendos de build, com condição de saída |
| [0003](0003-grafo-de-modulos.md) | Grafo de módulos e regras de dependência |
| [0004](0004-leitor-xml-dos-feeds.md) | Leitor XML dos feeds: xmlutil (`KtXmlReader`) em modo tolerante |
| [0005](0005-navegacao.md) | Navegação: abas Biblioteca e Episódios; Downloads vira filtro, Player só pelo mini player, descoberta no "+" |
| [0006](0006-preferencias.md) | Preferências do usuário: multiplatform-settings atrás de `PreferencesRepository` |
| [0007](0007-reordenar-listas.md) | Reordenar listas arrastando: Reorderable, com alça e ações de acessibilidade |

## Modelo

```markdown
# ADR NNNN — Título

- **Status:** proposta | aceita | substituída por ADR NNNN
- **Data:** AAAA-MM-DD
- **Fase:** item do roadmap

## Contexto
O problema e as restrições, com evidência (arquivo, linha, medição).

## Decisão
O que foi escolhido e as alternativas consideradas.

## Consequências
O que muda, o que fica mais difícil e como saber se a decisão precisa ser revista.
```
