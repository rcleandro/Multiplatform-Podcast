# CLAUDE.md

Guidance for Claude Code in the Podcast KMP repository. Project context lives in [docs/CONTEXTO.md](docs/CONTEXTO.md)
and the work plan in [docs/ROADMAP_MELHORIAS.md](docs/ROADMAP_MELHORIAS.md) (phases 9+; phases 1–8 are in
`roadmap-kmp-podcast.md`).

## Working on a roadmap phase

- One branch per phase from an up-to-date `main` (`feature/phase-09-design-system`), one commit per sub-item, one
  merge request per phase.
- Each sub-item ships with its test. Run the new test against the old code (must fail) and against the fix (must pass).
- When a sub-item is done, add an "Implementado" note to its section in the roadmap and mark it ✔.
- Commits are plain Conventional Commits without a ticket (`feat(designsystem): …`, `fix(player): …`).

## Checks before a commit

`./gradlew :shared:detekt :shared:desktopTest` must pass (rule inherited from `GEMINI.md`). Do not commit with Detekt
violations unless the user explicitly allows it. After touching platform code, also build the affected app
(`:androidApp:assembleDebug`, `:desktopApp:compileKotlinJvm`, `:webApp:wasmJsBrowserDistribution`,
`:shared:iosSimulatorArm64Test`).

## Code rules

- New tests go in `commonTest` with hand-written fakes (no MockK there), so they run on Native and Wasm.
- Code, identifiers, comments and log messages in English. User-facing text only in `composeResources`
  (`values`, `values-pt`, `values-es`), never in Kotlin; view models emit `StringResource`s, screens resolve them.
- No loose numbers: named `SNAKE_CASE` constants; spacing and shared sizes from design system tokens; a size owned
  by one composable is a named `val` inside it. No `Color(...)`, `.sp` or `.copy(fontSize = …)` in features.
- Platform files keep the `.android.kt` / `.ios.kt` suffixes; `MatchingDeclarationName` stays disabled in `detekt.yml`.
- Use `_` for unused catch variables and empty `{}` blocks without "No-op" comments.
- Never log or send to analytics a feed URL (private feeds carry access tokens), only the host.
