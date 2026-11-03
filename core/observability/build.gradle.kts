plugins {
    alias(libs.plugins.podcast.kmp.library)
    alias(libs.plugins.podcast.kmp.watch)
}

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.core.observability"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
