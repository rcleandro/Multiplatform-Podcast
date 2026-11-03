plugins {
    alias(libs.plugins.podcast.feature)
}

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.feature.library"
    }

    sourceSets {
        getByName("composeMain").dependencies {
            implementation(libs.reorderable)
        }
    }
}
