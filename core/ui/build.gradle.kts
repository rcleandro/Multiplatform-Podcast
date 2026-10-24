plugins {
    alias(libs.plugins.podcast.kmp.library)
    alias(libs.plugins.podcast.kmp.compose)
}

// Presentation shared by the features: user-facing texts (Res), EpisodeListItem and relative time.
kotlin {
    android {
        namespace = "br.com.carvalho.podcast.core.ui"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:designsystem"))
            api(project(":domain"))
            implementation(project(":core:common"))
            api(libs.components.resources)
            implementation(libs.kotlinx.datetime)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.filekit.dialogs.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val desktopTest = getByName("desktopTest") {
            dependencies {
                implementation(libs.ui.test)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "br.com.carvalho.podcast.core.ui.generated.resources"
}
