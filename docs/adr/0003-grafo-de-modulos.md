# ADR 0003 — Grafo de módulos

- **Status:** aceita
- **Data:** 2026-10-03
- **Fase:** 11.1 do [roadmap](../ROADMAP_MELHORIAS.md)

## Contexto

Quase todo o código vive no `:shared`, separado só por pacotes, e nada impede um pacote de depender de outro.
O levantamento de imports (03/10/2026) mostrou:

- `domain` importa `data.mapper` e `data.remote` (os casos de uso convertem RSS em modelo de domínio);
- os modelos de domínio importam `androidx.compose.runtime.Immutable`;
- `core/di` conhece todos os pacotes, inclusive as features;
- as telas leem os textos do `Res` do `:shared`;
- os SDKs do Firebase no iOS só fecham o link dentro do framework final, onde estão os pods do CocoaPods.

## Decisão

```
:core:common         dispatchers, constantes (AppConfig), tempo, sistema de arquivos, AppError
:core:observability  interfaces Logger, Analytics e CrashReporter, mais o logger Kermit; sem Firebase
:core:designsystem   tema, tokens e componentes (fase 9)
:core:ui             apresentação compartilhada pelas features: textos (Res), EpisodeListItem, RelativeTime, amostras de preview
:core:network        HttpClient e engines por plataforma, Json
:core:database       Room: banco, entidades, DAOs, migrações e schemas
:core:player         implementações do AudioPlayer, MediaService (Android), sessões de áudio (iOS, Web)
:core:testing        fakes compartilhados pelos testes
:domain              modelos, interfaces de repositório, AudioPlayer, EpisodeDownloader, casos de uso
:data                repositórios, feed RSS, downloader, mappers
:feature:library  :feature:podcast  :feature:episode  :feature:search  :feature:downloads  :feature:player
:shared              App, navegação raiz, montagem do Koin, implementações Firebase, framework iOS (CocoaPods)
:androidApp  :desktopApp  :webApp  iosApp/
```

Regras, verificadas no build pela 11.5:

| Módulo | Pode depender de |
|---|---|
| `:core:common` | nenhum módulo do projeto |
| `:core:observability`, `:core:designsystem`, `:core:network`, `:core:database` | `:core:common` |
| `:core:ui` | `:core:designsystem`, `:domain`, `:core:common` |
| `:domain` | `:core:common`, `:core:observability` |
| `:core:player` | `:domain`, `:core:common`, `:core:observability` |
| `:data` | `:domain`, `:core:database`, `:core:network`, `:core:common`, `:core:observability` |
| `:feature:*` | `:domain`, `:core:ui`, `:core:designsystem`, `:core:common`, `:core:observability` — **nunca** outra feature, `:data`, `:core:database`, `:core:network` ou `:core:player` |
| `:core:testing` | `:domain`, `:core:common`, `:core:observability` (só em `commonTest`) |
| `:shared` | todos |

Decisões menores:

- **Firebase fica no `:shared`.** `:core:observability` só declara interfaces; as implementações Firebase
  (Android e iOS) moram no `:shared`, onde estão os pods, e entram pelo Koin. Desktop e Web recebem as
  implementações que só registram em log. Assim os testes de cada módulo não dependem de Firebase.
- **`PagingData` no domínio é aceito.** É do `androidx.paging:paging-common`, uma biblioteca KMP sem Android.
- **Log e `@Serializable` no domínio são aceitos.** Os casos de uso registram em log pelo `AppLogger` do
  `:core:observability`, que não conhece o Firebase, e `Episode` é serializável porque a fila do player é salva
  em JSON; as duas bibliotecas são KMP puras.
- **Estabilidade do Compose fora do domínio.** Os modelos perdem `@Immutable`; um arquivo de configuração de
  estabilidade nos módulos de UI declara os modelos como estáveis.
- **Um `Res` por módulo de UI.** Os textos usados por várias features ficam em `:core:ui`; texto de uma feature
  só pode migrar para a própria feature depois, sem pressa.

## Consequências

- Mexer numa feature recompila só ela e o `:shared`, não o projeto todo.
- Uma feature que precise de algo de outra feature passa por `:domain` ou `:core:ui`, nunca por import direto.
- O `:shared` continua grande no começo (navegação e DI); a 11.10 enxuga a navegação.
- Cada módulo novo usa os convention plugins da 10.3; módulos sem UI usam só `podcast.kmp.library`.
