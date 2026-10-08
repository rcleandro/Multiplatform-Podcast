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
