plugins {
    alias(libs.plugins.podcast.kmp.library)
}

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.core.common"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
