# Instruções do Projeto - Podcast KMP

Este arquivo contém diretrizes mandatórias para o comportamento do agente Gemini CLI neste repositório.

## Workflow de Commits
- **Validação Estática Obrigatória:** Antes de realizar QUALQUER commit (seja via diretiva explícita ou como parte de uma tarefa), você deve obrigatoriamente executar o comando `./gradlew :shared:detekt`.
- **Bloqueio de Commit:** Se o Detekt encontrar violações, você não deve prosseguir com o commit. Em vez disso, relate os erros, proponha as correções e, após a aprovação/correção, execute o Detekt novamente antes de tentar o commit.
- **Exceção:** Somente ignore esta regra se o usuário explicitamente disser "pode commitar mesmo com erros no detekt" ou similar.

## Padrões de Código
- **Idioma:** Responda sempre em Português do Brasil.
- **Nomenclatura KMP:** Aceite arquivos com sufixos de plataforma (ex: `.android.kt`, `.ios.kt`) mesmo que o Detekt reclame (a regra `MatchingDeclarationName` deve permanecer desativada no `detekt.yml`).
- **Idiomatismo Kotlin:** Use `_` para variáveis de exceção não utilizadas e blocos vazios `{}` sem comentários "No-op".
