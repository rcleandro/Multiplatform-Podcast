# Podcast KMP

A podcast player for **Android** (with Android Auto), **iOS**, **Desktop** (macOS, Windows, Linux) and **Web**
(Wasm), built from one Kotlin Multiplatform codebase with Compose Multiplatform. Add a podcast by its RSS feed,
stream or download episodes, and pick up where you left off.

- Project context: [docs/CONTEXTO.md](docs/CONTEXTO.md)
- Work plan: [docs/ROADMAP_MELHORIAS.md](docs/ROADMAP_MELHORIAS.md)
- Design system: [docs/podcast-design-system.html](docs/podcast-design-system.html) and [ADR 0001](docs/adr/0001-identidade-visual.md)
- Decisions: [docs/adr](docs/adr)

## Modules

| Module | What it holds |
|---|---|
| `:shared` | Domain, data (Room, Ktor, RSS), screens and navigation; builds the iOS framework through CocoaPods |
| `:core:designsystem` | Theme, tokens, fonts and UI components |
| `:androidApp`, `:desktopApp`, `:webApp`, `iosApp/` | Thin app shells |
| `build-logic/` | Convention plugins (`podcast.kmp.library`, `podcast.kmp.compose`) |

## Requirements

- **JDK 21**
- **Android Studio** with the Kotlin Multiplatform plugin
- **Xcode** and **CocoaPods** (`brew install cocoapods`) for iOS; if `pod` fails after a Ruby update, run
  `brew reinstall cocoapods`
- Firebase config files, not committed: `androidApp/google-services.json` and
  `iosApp/iosApp/GoogleService-Info.plist`. In CI they come from the `GOOGLE_SERVICES_JSON` and
  `GOOGLE_SERVICE_INFO_PLIST` secrets (plain text or base64).

After cloning, enable the pre-commit hook (Detekt and desktop tests):

```bash
git config core.hooksPath config/hooks
```

## Build and run

```bash
./gradlew :androidApp:installDebug                 # Android
./gradlew :desktopApp:run                          # Desktop
./gradlew :webApp:wasmJsBrowserDevelopmentRun      # Web
cd iosApp && pod install && open iosApp.xcworkspace   # iOS (open the workspace, not the project)
```

## Checks

```bash
./gradlew :shared:desktopTest :core:designsystem:desktopTest   # unit and UI tests (JVM)
./gradlew :shared:iosSimulatorArm64Test                        # tests on the iOS simulator
./gradlew :shared:detekt :core:designsystem:detekt             # static analysis
./gradlew :androidApp:lintDebug                                # Android lint (baseline in androidApp/)
```

Design system snapshots are recorded on Linux by the **Record snapshots** workflow and verified in CI.
