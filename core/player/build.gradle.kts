plugins {
    alias(libs.plugins.podcast.kmp.library)
}

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.core.player"
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(project(":core:observability"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.session)
            // Chromecast (18.17): the receiver Google hosts for everyone, no Cast developer account.
            implementation(libs.media3.cast)
            implementation(libs.kotlinx.coroutines.guava)
            implementation(libs.koin.android)
        }
        val desktopMain = getByName("desktopMain") {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                val javafxVersion = libs.versions.javafx.get()

                val os = org.gradle.internal.os.OperatingSystem.current()
                val arch = System.getProperty("os.arch").lowercase()
                val classifier = when {
                    os.isMacOsX -> if (arch.contains("aarch64") || arch.contains("arm64")) "mac-aarch64" else "mac"
                    os.isWindows -> "win"
                    os.isLinux -> "linux"
                    else -> "mac"
                }

                // JavaFX ships per-OS jars; the classifier picks the one for the machine that builds. Exposed so the
                // app's window can show the player's video (javafx-swing, 19.10).
                listOf(libs.javafx.media, libs.javafx.graphics, libs.javafx.base, libs.javafx.swing).forEach {
                    api("${it.get().module}:$javafxVersion:$classifier")
                }
            }
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
        val androidHostTest = getByName("androidHostTest") {
            dependencies {
                implementation(project(":core:testing"))
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
