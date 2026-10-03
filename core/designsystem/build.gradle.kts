plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.roborazzi)
}

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.core.designsystem"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources { enable = true }
        withHostTest { isIncludeAndroidResources = true }
    }

    jvm("desktop")

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val nonAndroidMain by creating { dependsOn(commonMain.get()) }
        iosMain.get().dependsOn(nonAndroidMain)
        wasmJsMain.get().dependsOn(nonAndroidMain)
        val desktopMain by getting { dependsOn(nonAndroidMain) }

        commonMain.dependencies {
            api(libs.runtime)
            api(libs.foundation)
            api(libs.material3)
            api(libs.compose.ui)
            implementation(libs.components.resources)
            implementation(libs.composeIconsExtended)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ui.test)
        }
        val androidHostTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.roborazzi)
                implementation(libs.roborazzi.compose)
                implementation(libs.androidx.compose.ui.test.junit4)
                implementation(libs.androidx.compose.ui.test.manifest)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "br.com.carvalho.podcast.core.designsystem.generated.resources"
}

// Reference images live in the repository and are recorded on Linux by the "Record snapshots" workflow:
// Robolectric renders gradients and anti-aliasing differently on macOS, so images recorded on a Mac never match CI.
roborazzi {
    outputDir.set(file("snapshots"))
}
