plugins {
    alias(libs.plugins.podcast.kmp.library)
}

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.core.network"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:observability"))
            api(libs.ktor.client.core)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.logging)
            implementation(libs.ktor.encoding)
            api(libs.kotlinx.serialization)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}
