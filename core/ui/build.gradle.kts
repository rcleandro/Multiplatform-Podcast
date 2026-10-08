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
            implementation(libs.composeIconsExtended)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "br.com.carvalho.podcast.core.ui.generated.resources"
}
