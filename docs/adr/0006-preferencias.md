# ADR 0006 — Preferências do usuário

- **Status:** aceita
- **Data:** 2026-10-06
- **Fase:** 24.4 do [roadmap](../ROADMAP_MELHORIAS.md); a 18.5 (Configurações) usa a mesma base

## Contexto

O app não guarda nenhuma escolha do usuário entre aberturas. A 24.4 precisa lembrar o layout e a ordem da
biblioteca, e a 18.5 vai somar tema, saltos, velocidade padrão, download automático e telemetria. São poucos valores
simples, lidos no início e mudados raramente, nas quatro plataformas, inclusive a Web.

## Decisão

**multiplatform-settings** (`com.russhwolf:multiplatform-settings` 1.3.0), chave-valor sobre o armazenamento de cada
plataforma: `SharedPreferences` no Android, `NSUserDefaults` no iOS, `localStorage` na Web e `java.util.prefs` no
Desktop, num nó próprio (`br/com/carvalho/podcast`), porque o `Settings()` sem argumentos grava na raiz, compartilhada
por todos os apps Java do usuário.

O domínio só conhece `PreferencesRepository`; a implementação (`SettingsPreferencesRepository`, no `:data`) guarda
enums pelo nome, volta ao padrão quando lê um valor desconhecido e expõe cada preferência como `StateFlow`. Como nem
todo `Settings` pode ser observado (o da Web não avisa mudanças), o repositório é o único que escreve e mantém os
flows ele mesmo, como singleton do Koin.

Alternativas:
- **DataStore KMP:** observável e transacional, mas sem versão para Wasm/JS; a Web ficaria com outra implementação.
- **Tabela no Room:** sem dependência nova, mas cada preferência nova viraria migração, e o banco já é o lugar dos
  dados do usuário, não das escolhas de tela.

## Consequências

- Nova dependência no `:data` (e o `-no-arg` no `:shared`, que monta o `Settings` de cada plataforma); nos testes, o
  `MapSettings` do `multiplatform-settings-test` faz o papel de armazenamento.
- Valores grandes ou listas não cabem aqui: a ordem personalizada da biblioteca vai para o banco (coluna de posição).
- Se alguma preferência precisar ser escrita por outro processo (um widget, 19.7), o "único escritor" deixa de valer
  e a decisão volta a ser revista.
