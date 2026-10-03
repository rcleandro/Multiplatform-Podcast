plugins {
    alias(libs.plugins.podcast.kmp.library)
}

// Fakes shared by the tests of several modules; only ever a test dependency.
kotlin {
    android {
        namespace = "br.com.carvalho.podcast.core.testing"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:observability"))
        }
    }
}
