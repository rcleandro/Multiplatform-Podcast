rootProject.name = "Podcast"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven("https://redirector.kotlinlang.org/maven/compose-dev")
        ivy {
            url = uri("https://nodejs.org/dist")
            patternLayout {
                artifact("v[revision]/[artifact]-v[revision]-[classifier].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeGroup("org.nodejs")
            }
        }
        ivy {
            url = uri("https://github.com/yarnpkg/yarn/releases/download")
            patternLayout {
                artifact("v[revision]/[artifact]-v[revision].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeGroup("com.yarnpkg")
            }
        }
        // Binaryen (Wasm optimizer) for the Web production build; the Kotlin plugin adds this repository
        // itself, but PREFER_SETTINGS ignores repositories declared outside this file.
        ivy {
            url = uri("https://github.com/WebAssembly/binaryen/releases/download")
            patternLayout {
                artifact("version_[revision]/[artifact]-version_[revision]-[classifier].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeGroup("com.github.webassembly")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":shared")
include(":core:common")
include(":core:database")
include(":core:designsystem")
include(":core:network")
include(":core:observability")
include(":core:player")
include(":core:testing")
include(":core:ui")
include(":domain")
include(":data")
include(":feature:library")
include(":feature:podcast")
include(":feature:episode")
include(":feature:search")
include(":feature:player")
include(":androidApp")
include(":desktopApp")
include(":webApp")
include(":benchmark")
