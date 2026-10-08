plugins {
    alias(libs.plugins.podcast.feature)
    alias(libs.plugins.roborazzi)
}

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.feature.player"
        withHostTest { isIncludeAndroidResources = true }
    }

    sourceSets {
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
    }
}

// The player in each screen type the app supports (21.8). Recorded on Linux by the "Record snapshots" workflow,
// like the design system's: Robolectric renders differently on macOS.
roborazzi {
    outputDir.set(file("snapshots"))
}
