plugins {
    alias(libs.plugins.podcast.kmp.library)
    alias(libs.plugins.podcast.kmp.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlin.cocoapods)
}

kover {
    reports {
        total {
            log {
                onCheck = true
            }
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "br.com.carvalho.podcast.shared"
}

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.shared"
        withHostTest {}
    }

    cocoapods {
        version = "1.0"
        summary = "Shared module for Podcast app"
        homepage = "https://github.com/rcleandro/Multiplatform-Podcast"
        ios.deploymentTarget = "18.2"
        framework {
            baseName = "Shared"
            isStatic = true
            linkerOpts("-ObjC")
        }
        pod("FirebaseAnalytics") {
            version = "~> 11.0"
        }
        pod("FirebaseCrashlytics") {
            version = "~> 11.0"
        }
        pod("FirebaseCore") {
            version = "~> 11.0"
        }
        pod("FirebaseInstallations") {
            version = "~> 11.0"
        }
        pod("GoogleUtilities") {
            version = "~> 8.0"
        }
        pod("nanopb") {
            version = "~> 3.0"
        }
        pod("PromisesObjC") {
            version = "~> 2.0"
            moduleName = "FBLPromises"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:designsystem"))

            // Compose
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.components.resources)
            implementation(libs.composeIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)

            // Adaptive
            implementation(libs.material3.adaptive)
            implementation(libs.material3.adaptive.layout)
            implementation(libs.material3.adaptive.navigation)
            implementation(libs.material3.adaptive.navigation.suite)

            // Room 3
            implementation(libs.room3.runtime)
            api(libs.room3.paging)


            // Paging
            implementation(libs.paging.common)
            implementation(libs.paging.compose)

            // Ktor 3
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.logging)
            implementation(libs.ktor.encoding)

            // Koin 4
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Kotlinx
            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            // Okio
            implementation(libs.okio)

            // Decompose
            implementation(libs.decompose)
            implementation(libs.decompose.compose)

            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodel)

            // Imagem
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // Logging
            implementation(libs.kermit)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            implementation(libs.sqlite.bundled)

            implementation(libs.media3.exoplayer)
            implementation(libs.media3.session)
            implementation(libs.kotlinx.coroutines.guava)

            implementation(project.dependencies.platform(libs.firebase.bom))
        }

        // Firebase implementations shared by Android and iOS; Desktop and Web only log.
        val firebaseMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.firebase.common)
                implementation(libs.firebase.analytics)
                implementation(libs.firebase.crashlytics)
            }
        }
        androidMain.get().dependsOn(firebaseMain)

        val iosMain by getting {
            dependsOn(firebaseMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqlite.bundled)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(compose.desktop.currentOs)
                implementation(libs.sqlite.bundled)

                val javafxVersion = libs.versions.javafx.get()

                val os = org.gradle.internal.os.OperatingSystem.current()
                val arch = System.getProperty("os.arch").lowercase()
                val classifier = when {
                    os.isMacOsX -> if (arch.contains("aarch64") || arch.contains("arm64")) "mac-aarch64" else "mac"
                    os.isWindows -> "win"
                    os.isLinux -> "linux"
                    else -> "mac"
                }

                // JavaFX ships per-OS jars; the classifier picks the one for the machine that builds.
                listOf(libs.javafx.media, libs.javafx.graphics, libs.javafx.base).forEach {
                    implementation("${it.get().module}:$javafxVersion:$classifier")
                }
            }
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.sqlite.web)
            implementation(libs.okio.fakefilesystem)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
            implementation(libs.ui.test)
            implementation(libs.okio.fakefilesystem)
        }

        val jvmCommonTest by creating {
            dependsOn(commonTest.get())
            dependencies {
                implementation(libs.mockk)
                implementation(libs.androidx.sqlite.bundled.jvm)
            }
        }

        val androidHostTest by getting {
            dependsOn(jvmCommonTest)
        }

        val desktopTest by getting {
            dependsOn(jvmCommonTest)
        }
    }
}

// ponytail: the Kotlin CocoaPods plugin only raises pod deployment targets to iOS 12 (KT-57741), but Xcode 26+
// rejects anything below 15, which breaks the Android Studio sync and the iOS build. Patch the generated Podfile
// until the plugin raises it or Firebase 12 (minimum iOS 15) replaces the 11.x pods.
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.PodGenTask>().configureEach {
    doLast {
        val file = podfile.get()
        file.writeText(
            file.readText()
                .replace(
                    "deployment_target_major < 12 || (deployment_target_major == 12",
                    "deployment_target_major < 15 || (deployment_target_major == 15"
                )
                .replace("\"#{12}.#{0}\"", "\"#{15}.#{0}\"")
        )
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspCommonMainMetadata", libs.room3.compiler)
    add("kspAndroid", libs.room3.compiler)
    add("kspIosArm64", libs.room3.compiler)
    add("kspIosSimulatorArm64", libs.room3.compiler)
    add("kspDesktop", libs.room3.compiler)
    add("kspWasmJs", libs.room3.compiler)
}
