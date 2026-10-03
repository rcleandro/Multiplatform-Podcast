plugins {
    alias(libs.plugins.podcast.kmp.library)
}

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.domain"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:common"))
            implementation(project(":core:observability"))
            api(libs.paging.common)
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
