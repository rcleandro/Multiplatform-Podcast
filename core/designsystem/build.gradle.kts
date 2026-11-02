plugins {
    alias(libs.plugins.podcast.kmp.library)
    alias(libs.plugins.podcast.kmp.compose)
    alias(libs.plugins.roborazzi)
}

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.core.designsystem"
        withHostTest { isIncludeAndroidResources = true }
    }

    sourceSets {
        val nonAndroidMain = create("nonAndroidMain") { dependsOn(getByName("composeMain")) }
        wasmJsMain.get().dependsOn(nonAndroidMain)
        val desktopMain = getByName("desktopMain") { dependsOn(nonAndroidMain) }

        // ponytail: the feed HTML parser (iOS reads it too) builds Compose's AnnotatedString; a neutral span model
        // would drop this when the iOS framework stops linking Compose altogether (ADR 0015).
        commonMain.dependencies {
            implementation(libs.compose.ui)
            // Res is generated in commonMain; core:ui brings the library to iOS for its own texts anyway.
            implementation(libs.components.resources)
        }
        getByName("composeMain").dependencies {
            api(libs.runtime)
            api(libs.foundation)
            api(libs.material3)
            api(libs.compose.ui)
            api(libs.material3.adaptive)
            // The Icons object and the few icons it holds; the others the app uses are copied into `icons` (23.9).
            api(libs.material.icons.core)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        getByName("composeTest").dependencies {
            implementation(libs.ui.test)
        }
        val androidHostTest = getByName("androidHostTest") {
            dependencies {
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.roborazzi)
                implementation(libs.roborazzi.compose)
                implementation(libs.androidx.compose.ui.test.junit4)
                implementation(libs.androidx.compose.ui.test.manifest)
            }
        }
        val desktopTest = getByName("desktopTest") {
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

// IosTokensTest writes the iOS app's copy of the theme with -Pios.tokens.write=true (ADR 0015) and checks it otherwise.
tasks.withType<Test>().configureEach {
    systemProperty("ios.tokens.write", providers.gradleProperty("ios.tokens.write").getOrElse("false"))
}
