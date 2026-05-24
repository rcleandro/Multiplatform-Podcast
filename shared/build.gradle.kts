plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
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
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "br.com.carvalho.podcast.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTest {}
    }

    jvm("desktop")

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        version = "1.0"
        summary = "Shared module for Podcast app"
        homepage = "https://github.com/leandro/Podcast"
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "Shared"
            isStatic = true
            linkerOpts("-ObjC")
        }
        pod("FirebaseAnalytics") {
            version = "~> 11.0"
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
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

            // Firebase
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.common)
            implementation(libs.firebase.analytics)
        }

        val iosMain by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqlite.bundled)

                // Firebase
                implementation(libs.firebase.common)
                implementation(libs.firebase.analytics)
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

                implementation("org.openjfx:javafx-media:$javafxVersion:$classifier")
                implementation("org.openjfx:javafx-graphics:$javafxVersion:$classifier")
                implementation("org.openjfx:javafx-base:$javafxVersion:$classifier")
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

// Custom task to sync Compose resources for Android assets (AGP 9.x compatibility)
val syncComposeResourcesForAndroid = tasks.register<Copy>("syncComposeResourcesForAndroid") {
    dependsOn("prepareComposeResourcesTaskForCommonMain")
    from(layout.buildDirectory.dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"))
    into(layout.buildDirectory.dir("generated/compose/androidAssets/composeResources/br.com.carvalho.podcast.shared"))
}

kotlin.sourceSets.getByName("androidMain").resources.srcDirs(
    syncComposeResourcesForAndroid.map { it.destinationDir.parentFile }
)
