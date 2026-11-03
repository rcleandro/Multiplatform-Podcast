plugins {
    alias(libs.plugins.podcast.kmp.library)
    alias(libs.plugins.podcast.kmp.compose)
}

// Presentation shared by the features: user-facing texts (Res) and relative time; EpisodeListItem and the other
// composables in composeMain.
kotlin {
    android {
        namespace = "br.com.carvalho.podcast.core.ui"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            implementation(project(":core:common"))
            api(libs.components.resources)
            implementation(libs.kotlinx.datetime)
        }
        getByName("composeMain").dependencies {
            api(project(":core:designsystem"))
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
