# Architecture Decision Records

One file per decision, numbered in order: `NNNN-titulo-curto.md`. A decision that replaces another sets the old
one to "substituída por ADR NNNN" instead of deleting it.

| ADR | Decisão |
|---|---|
| [0001](0001-identidade-visual.md) | Identidade visual "Sinal": cores, tipografia, tokens e ícone |
| [0002](0002-dependencias-instaveis-e-remendos.md) | Dependências instáveis e remendos de build, com condição de saída |

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
