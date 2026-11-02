plugins {
    alias(libs.plugins.podcast.kmp.library)
    alias(libs.plugins.podcast.kmp.watch)
}

// The Apple Watch's side of the shared code (ADR 0016): the graph of `:core:di` and small facades for the SwiftUI
// screens, as the features' view models cannot reach watchOS (their Compose resources have no watchOS). Its framework,
// WatchShared, goes into the watch app through embedAndSignAppleFrameworkForXcode, without CocoaPods.
kotlin {
    android {
        namespace = "br.com.carvalho.podcast.watch"
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>()
        .matching { it.konanTarget.family == org.jetbrains.kotlin.konan.target.Family.WATCHOS }
        .configureEach {
            binaries.framework {
                baseName = "WatchShared"
                isStatic = true
                // Swift sees the library's own types (Podcast, Episode) under their names.
                export(project(":domain"))
            }
        }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:di"))
            api(project(":domain"))
            implementation(libs.kotlinx.coroutines.core)
        }
        watchosMain.dependencies {
            implementation(libs.multiplatform.settings.no.arg)
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
