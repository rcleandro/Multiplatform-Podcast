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

Limites conhecidos:
- No Android, o `app_start` começa no `initKoin` (no `Application.onCreate`), e não no início do processo. As
  centenas de milissegundos antes disso ficam de fora: o Macrobenchmark da 17.6 mede o início completo.
- No iOS, os downloads vão pelo `URLSession` em segundo plano, e não pelo `KtorEpisodeDownloader`, então não geram
  `download`.
