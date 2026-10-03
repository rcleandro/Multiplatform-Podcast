# ADR 0002 — Dependências instáveis e remendos de build

- **Status:** aceita
- **Data:** 2026-10-03
- **Fase:** 10.7 do [roadmap](../ROADMAP_MELHORIAS.md)

## Contexto

O build tinha exceções sem explicação registrada: versões alpha, uma versão forçada e, desde a 9.x, contornos
para ferramentas locais. Sem o motivo e a condição de saída, ninguém sabe quando é seguro removê-las.

## Decisão

Cada exceção fica listada aqui, com o motivo e a condição para sair. Quem adicionar uma nova atualiza esta ADR.

| Exceção | Onde | Por que existe | Condição de saída |
|---|---|---|---|
| Room 3 `3.0.0-alpha05` e sqlite-web `2.7.0-alpha05` | `libs.versions.toml` | Único Room com suporte a Wasm (OPFS) | Room 3 estável; revisar a cada versão (Dependabot, 10.6) |
| `-Xexpect-actual-classes` | `podcast.kmp.library` | Classes `expect`/`actual` ainda são beta no Kotlin | Kotlin estabilizar o recurso, ou os `expect object` virarem interfaces injetadas (11.6) |
| Podfile sintético com pods em iOS 15 | `shared/build.gradle.kts` | O plugin CocoaPods do Kotlin só sobe os pods para iOS 12 (KT-57741), e o Xcode 26+ exige 15; sem isso o sync do Android Studio e o build iOS quebram | Plugin subir o mínimo, ou Firebase 12 (mínimo iOS 15) no lugar dos pods 11.x |
| Baseline do Detekt (189 achados) e do lint Android (1 erro, 7 avisos) | `config/detekt/baseline.xml`, `androidApp/lint-baseline.xml` | Código antigo; só problemas novos quebram o build | Zerar na 17.1; os itens do lint estão nas 20.7 (busca por voz do Android Auto) e 22.2 (serviço exportado) |

## Removido nesta decisão

- **`force("org.jetbrains.skiko:skiko:0.9.43")`** no `:shared`. Valia só dentro do `:shared`: o app Desktop
  resolvia `0.144.6`, então testes e app rodavam versões diferentes. Sem o `force`, o framework iOS faz o link,
  o app iOS compila pelo workspace e roda no simulador, e os testes JVM e Android passam (verificado em
  03/10/2026).
- **Task `syncComposeResourcesForAndroid`** (10.3): `androidResources.enable = true` no convention plugin
  empacota os recursos do Compose sem cópia manual.

## Consequências

- Remover uma linha desta tabela exige testar o que a coluna "Por que existe" descreve.
- O Dependabot (10.6) abre PRs para as versões alpha; a revisão desses PRs é o momento de checar a saída.
