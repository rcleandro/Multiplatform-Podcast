# Métricas de saúde

Cada medição sai como uma linha de log de nível info, com a tag `Metrics`:

```
metric=<nome> ms=<duração> chave=valor…
```

Ela aparece no console (Logcat, Xcode, terminal do Desktop, console do navegador) e, no Android e no iOS, como
breadcrumb do Crashlytics, se a telemetria estiver ligada (16.4). Os campos levam só contagens, tamanhos e resultados,
nunca URL nem título. Nada de SDK de desempenho: os números de referência vêm dos benchmarks da 17.6 e da 23.1.

Para ver no Android: `adb logcat -s Metrics`.

| Métrica | De → até | Campos | Orçamento |
|---|---|---|---|
| `app_start` | `initKoin` → primeira lista da biblioteca vinda do banco (uma vez por processo) | `podcasts` | ≤ 1 500 ms a frio num celular intermediário |
| `time_to_audio` | pedido de tocar → primeiro som do player | `source` (`local` ou `stream`) | `local` ≤ 500 ms; `stream` ≤ 2 000 ms em 4G |
| `feed_refresh` | início → fim da atualização de um podcast | `outcome` (`updated`, `unchanged`, `failed`) | `unchanged` ≤ 1 000 ms; `updated` ≤ 3 000 ms |
| `refresh_all` | início → fim de "atualizar tudo" | `podcasts`, `failed` | ≤ 10 s para 30 podcasts |
| `download` | início → arquivo completo no lugar | `bytes` | Sem orçamento fixo: depende da rede. Serve para comparar a vazão (`bytes / ms`) entre versões |

Os orçamentos são metas iniciais, ainda não medidas num aparelho. Quando os benchmarks da 17.6 e da 23.1 existirem,
os números deles substituem estes, e uma piora acima do orçamento passa a ser regressão.

## Início do app no Android (Macrobenchmark)

`StartupBenchmark` (módulo `:benchmark`) mede o início a frio até o primeiro quadro (`timeToInitialDisplay`) num
build de release (`benchmark`, assinado com a chave de debug), 10 vezes por cenário. Precisa de um aparelho
conectado e fica fora do CI:

```
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

| Data | Aparelho | Sem compilação (mediana) | Parcialmente compilado (mediana) |
|---|---|---|---|
| 08/10/2026 | Razr 60, Android 16 | 843 ms (804–877) | 741 ms (701–915) |
| 23/10/2026 | Razr 60, Android 16 (R8 e baseline profile, 20.1) | 548 ms (528–644) | 450 ms (427–534) |
| 24/10/2026 | Razr 60, Android 16, pelo cabo USB (profile gravado de novo, 23.5) | 765 ms (750–883) | 647 ms (568–774) |

O R8 da 20.1 cortou cerca de 300 ms dos dois cenários. O baseline profile (`androidApp/src/main/baseline-prof.txt`)
é o que o aparelho compila na instalação; o cenário parcialmente compilado mede o app com ele. Para gerá-lo de novo
depois de mudar o caminho até a biblioteca, com o Razr conectado:

```
./gradlew :benchmark:connectedBaselineProfileAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=br.com.carvalho.podcast.benchmark.BaselineProfileGenerator
```

e copiar o `…-startup-prof.txt` de `benchmark/build/outputs/connected_android_test_additional_output/` para
`androidApp/src/main/baseline-prof.txt`. Ele roda no build `baselineProfile`, que é o `benchmark` sem R8: o R8 junta e
move classes, e um profile gravado no código encolhido não nomearia as classes do código-fonte. Orçamento:
≤ 1 000 ms sem compilação neste aparelho. Uma mudança que passe disso é regressão.

## Uso do app no Android (Macrobenchmark, 23.1)

`UsageBenchmark` (módulo `:benchmark`) mede o que vem depois da abertura, no build `benchmark` (R8 e baseline
profile): a rolagem da aba Episódios (5 arremessos para baixo) e a abertura do player a partir do mini player, com
`FrameTimingMetric`, e a memória do processo depois da rolagem, com `MemoryUsageMetric`. Roda na biblioteca do próprio
aparelho, com o app em português e um episódio no mini player:

```
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=br.com.carvalho.podcast.benchmark.UsageBenchmark
```

`frameOverrunMs` é quanto um quadro passa do prazo da tela: negativo é folga. O P99 varia de uma execução para outra
(+1,1 e +6,7 ms na rolagem, no mesmo dia), daí a folga nos orçamentos.

| Medição | Orçamento (Razr 60) | 24/10/2026: 7 podcasts, 13.077 episódios |
|---|---|---|
| Rolagem: `frameOverrunMs` | P90 ≤ 0 ms, P99 ≤ 16 ms | P50 −9,0 · P90 −3,0 · P99 +6,7 ms |
| Rolagem: `frameDurationCpuMs` | P99 ≤ 24 ms | P50 4,7 · P90 8,0 · P99 16,3 ms |
| Abertura do player: `frameOverrunMs` | P90 ≤ 0 ms, P99 ≤ 16 ms | P50 −8,1 · P90 −5,9 · P99 +10,8 ms |
| Memória depois de rolar: heap Java | ≤ 100 MB | 73 MB |
| Memória depois de rolar: RSS anônima | ≤ 220 MB | 160 MB |

A memória é medida na biblioteca real do aparelho, não nos 50 podcasts que a 23.1 previa: assinar 50 feeds no
celular do usuário só para medir mudaria os dados dele. O número de episódios (13.077) é o que mais pesa na lista e
na busca. Ainda sem medição no iPhone: a interface do iOS será reescrita em SwiftUI na fase 25, e medir o Compose dele
agora mediria o que vai sair.

Cuidado ao rodar: o Gradle desinstalava o app depois dos testes no aparelho, apagando a biblioteca;
`android.injected.androidTest.leaveApksInstalledAfterRun=true` no `gradle.properties` impede isso desde 24/10/2026.

## Resultados por versão (23.11)

Cada medição fica em [perf-results.csv](perf-results.csv), uma linha por métrica, com a data, o commit medido
(`-dirty` se havia mudança não commitada), a versão do app e o aparelho (ou o alvo, no teste de parsing). Logo depois
de rodar um Macrobenchmark no aparelho, ou os testes do `:data`:

```
python3 .github/scripts/perf_results.py record
```

e commitar a linha nova junto. O script lê o JSON do Macrobenchmark em `benchmark/build/outputs/…` e os relatórios do
`LargeFeedParsingTest` em `data/build/test-results/`; ele grava o que estiver lá, então rode logo depois do teste,
para não gravar um relatório velho com o commit novo. Os orçamentos das tabelas acima estão no próprio script
(`BUDGETS`).

No CI, o job de testes JVM roda `perf_results.py check`: avisa (`::warning::`, sem falhar o build) quando o último
resultado de uma série passa do orçamento, e põe no resumo do job o tempo de parsing daquela execução. O
Macrobenchmark não roda no CI, então o aviso só vê o que foi gravado do aparelho. Para achar o commit de uma piora,
compare as linhas da série no CSV e meça os commits do meio.

Limites conhecidos:
- No Android, o `app_start` começa no `initKoin` (no `Application.onCreate`), e não no início do processo. As
  centenas de milissegundos antes disso ficam de fora: o Macrobenchmark da 17.6 mede o início completo.
- No iOS, os downloads vão pelo `URLSession` em segundo plano, e não pelo `KtorEpisodeDownloader`, então não geram
  `download`.
