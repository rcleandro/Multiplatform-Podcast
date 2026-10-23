plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "podcast.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "podcast.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("appVersion") {
            id = "podcast.app.version"
            implementationClass = "AppVersionConventionPlugin"
        }
        register("feature") {
            id = "podcast.feature"
            implementationClass = "FeatureConventionPlugin"
        }
    }
}
