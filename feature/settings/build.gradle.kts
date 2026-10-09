plugins {
    alias(libs.plugins.podcast.feature)
}

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.feature.settings"
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.okio.fakefilesystem)
        }
    }
}
