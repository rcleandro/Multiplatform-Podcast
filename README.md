# 🎙️ Podcast KMP

A modern, cross-platform podcast player application built with **Kotlin Multiplatform (KMP)**. This project demonstrates the power of a single codebase targeting **Android**, **iOS**, **Desktop (JVM)**, and **Web (Wasm)**.

## 🚀 Key Features

- **Multi-Platform Support**: Native feel on Android, iOS, Desktop (macOS/Windows/Linux), and Web.
- **RSS Feed Integration**: Add any podcast via its RSS URL.
- **Offline Downloads**: Download episodes for offline listening.
- **Persistent Playback**: Resume from where you left off.
- **Adaptive UI**: Responsive design using **Compose Multiplatform**.

## 🛠️ Technology Stack

- **[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)**: Shared UI components across all platforms.
- **[Room 3 (KMP)](https://developer.android.com/kotlin/multiplatform/room)**: Local database for cross-platform data persistence.
- **[Ktor 3](https://ktor.io/)**: Asynchronous HTTP client for fetching RSS feeds and downloading audio.
- **[Koin 4](https://insert-koin.io/)**: Dependency injection for a modular and testable architecture.
- **[Decompose](https://github.com/arkivanov/Decompose)**: Life-cycle aware navigation and component decomposition.
- **[Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)**: For handling XML (RSS) and JSON.

## 📐 Architecture

The project follows **Clean Architecture** principles combined with **MVVM**:

```
:shared
 ├── commonMain/         # Core logic, domain models, and shared UI
 ├── androidMain/        # Android-specific implementations (Media3 Player)
 ├── iosMain/            # iOS-specific implementations (AVFoundation Player)
 ├── desktopMain/        # Desktop-specific implementations (JavaFX Media Player)
 └── wasmJsMain/         # Web-specific implementations (HTML5 Audio)

:androidApp              # Android target wrapper
:iosApp                  # iOS target (Xcode project)
:desktopApp              # Desktop target wrapper
:webApp                  # Web target (Wasm)
```

## 🏗️ Getting Started

### Prerequisites

- **JDK 17 or 21**
- **Android Studio** (Koala or newer) or **IntelliJ IDEA**
- **Xcode** (for iOS development)
- **KMP Support Plugin** installed in your IDE

### Build and Run

- **Android**: `./gradlew :androidApp:installDebug`
- **Desktop**: `./gradlew :desktopApp:run`
- **Web (Wasm)**: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
- **iOS**: Open the `iosApp` folder in Xcode and run the project.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

*Developed as a showcase of modern Kotlin Multiplatform capabilities.*
