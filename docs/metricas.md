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

Limites conhecidos:
- No Android, o `app_start` começa no `initKoin` (no `Application.onCreate`), e não no início do processo. As
  centenas de milissegundos antes disso ficam de fora: o Macrobenchmark da 17.6 mede o início completo.
- No iOS, os downloads vão pelo `URLSession` em segundo plano, e não pelo `KtorEpisodeDownloader`, então não geram
  `download`.
