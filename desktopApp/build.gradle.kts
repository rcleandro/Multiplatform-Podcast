import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.podcast.app.version)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":domain"))
            implementation(project(":core:ui"))
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.decompose)
            implementation(libs.components.resources)
        }
    }
}

compose.desktop {
    application {
        mainClass = "br.com.carvalho.podcast.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "br.com.carvalho.podcast"
            packageVersion = appVersion.name
            macOS { iconFile.set(project.file("icons/icon.icns")) }
            windows { iconFile.set(project.file("icons/icon.ico")) }
            linux { iconFile.set(project.file("icons/icon.png")) }
        }
    }
}
