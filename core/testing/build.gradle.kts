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
            api(project(":domain"))
            implementation(libs.kotlinx.coroutines.core)
        }
        // In-memory Room with the bundled driver: Android host tests run on the JVM, so both use the JVM artifact.
        val jvmCommonMain = create("jvmCommonMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.androidx.sqlite.bundled.jvm)
            }
        }
        androidMain.get().dependsOn(jvmCommonMain)
        val desktopMain = getByName("desktopMain") { dependsOn(jvmCommonMain) }
        iosMain.dependencies {
            implementation(libs.sqlite.bundled)
        }
    }
}
