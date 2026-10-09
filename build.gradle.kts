plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(
            files("${rootProject.projectDir}/config/detekt/detekt.yml") +
                if (path == ":core:testing") files("${rootProject.projectDir}/config/detekt/testing.yml") else files()
        )
        buildUponDefaultConfig = true
        allRules = false
        parallel = true
        source.setFrom(files("src/commonMain/kotlin", "src/firebaseMain/kotlin", "src/jvmCommonMain/kotlin", "src/androidMain/kotlin", "src/desktopMain/kotlin", "src/iosMain/kotlin", "src/wasmJsMain/kotlin"))
    }
    dependencies {
        "detektPlugins"(rootProject.libs.detekt.formatting)
    }
}

// Line coverage of the JVM tests (desktop), per module, without Compose UI: screens are checked by the snapshot and
// interaction tests of 17.4 instead. Each floor is the coverage the module had when it was set (17.2), so a drop
// fails `koverVerify`; raise it as tests are added. Goals: domain and data ≥ 90%, features ≥ 70%.
val coverageFloors = mapOf(
    ":core:common" to 29,
    ":core:database" to 60,
    ":core:designsystem" to 92,
    ":core:network" to 87,
    ":core:observability" to 92,
    ":core:player" to 46,
    ":core:ui" to 59,
    ":data" to 94,
    ":domain" to 90,
    ":feature:episode" to 84,
    ":feature:library" to 97,
    ":feature:player" to 97,
    ":feature:podcast" to 98,
    ":feature:search" to 87,
    ":shared" to 46,
)

subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "org.jetbrains.kotlinx.kover")
        configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
            reports {
                filters {
                    excludes {
                        annotatedBy(
                            "androidx.compose.runtime.Composable",
                            "androidx.compose.ui.tooling.preview.Preview",
                            "org.jetbrains.compose.ui.tooling.preview.Preview",
                        )
                        // Compose resources and lambdas, and the code Room generates for the database.
                        classes("*.generated.resources.*", "*ComposableSingletons*", "*_Impl", "*_Impl$*")
                    }
                }
                verify {
                    rule {
                        minBound(coverageFloors[path] ?: 0)
                    }
                }
            }
        }
    }
}
