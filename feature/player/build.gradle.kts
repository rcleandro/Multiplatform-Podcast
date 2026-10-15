plugins {
    alias(libs.plugins.podcast.feature)
}

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.feature.player"
    }

    sourceSets {
        commonMain.dependencies {
            // Dragging to reorder the queue (18.6, ADR 0007).
            implementation(libs.reorderable)
        }
        androidMain.dependencies {
            // The Cast button (18.17).
            implementation(libs.media3.cast)
        }
    }
}
