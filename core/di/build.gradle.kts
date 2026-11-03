plugins {
    alias(libs.plugins.podcast.kmp.library)
    alias(libs.plugins.podcast.kmp.watch)
}

// The app's object graph without its screens (ADR 0016): `:shared` adds the features' view models on top, and `:watch`
// uses it as it is.
kotlin {
    android {
        namespace = "br.com.carvalho.podcast.core.di"
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:common"))
            api(project(":core:database"))
            api(project(":core:network"))
            api(project(":core:observability"))
            api(project(":core:player"))
            api(project(":data"))
            api(project(":domain"))
            api(libs.koin.core)
            implementation(libs.ktor.client.core)
            implementation(libs.multiplatform.settings)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
