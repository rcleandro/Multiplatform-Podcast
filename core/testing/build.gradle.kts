plugins {
    alias(libs.plugins.podcast.kmp.library)
}

// Fakes and helpers shared by the tests of several modules; only ever a test dependency.
kotlin {
    android {
        namespace = "br.com.carvalho.podcast.core.testing"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:observability"))
            api(project(":core:database"))
            implementation(libs.kotlinx.coroutines.core)
        }
        // In-memory Room with the bundled driver: Android host tests run on the JVM, so both use the JVM artifact.
        val jvmCommonMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.androidx.sqlite.bundled.jvm)
            }
        }
        androidMain.get().dependsOn(jvmCommonMain)
        val desktopMain by getting { dependsOn(jvmCommonMain) }
        iosMain.dependencies {
            implementation(libs.sqlite.bundled)
        }
    }
}
