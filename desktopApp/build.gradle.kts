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
            // The name people see (Podcast.app, the Start menu); the data stays in ~/.podcast whatever it is.
            packageName = "Podcast"
            packageVersion = appVersion.name
            vendor = "Leandro Carvalho"
            macOS {
                bundleID = "br.com.carvalho.podcast"
                iconFile.set(project.file("icons/icon.icns"))
                // A Developer ID from CI secrets signs and notarizes the app; without it the DMG comes out unsigned.
                System.getenv("MACOS_SIGNING_IDENTITY")?.let { developerId ->
                    signing {
                        sign.set(true)
                        identity.set(developerId)
                    }
                    notarization {
                        appleID.set(System.getenv("MACOS_NOTARIZATION_APPLE_ID"))
                        password.set(System.getenv("MACOS_NOTARIZATION_PASSWORD"))
                        teamID.set(System.getenv("MACOS_NOTARIZATION_TEAM_ID"))
                    }
                }
            }
            windows {
                iconFile.set(project.file("icons/icon.ico"))
                // Fixed for good: a new MSI with the same one replaces the installed version instead of sitting beside it.
                upgradeUuid = "72F2FEC1-2566-4BCC-9217-524714B2B324"
                menuGroup = "Podcast"
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
                // Debian package names are lowercase.
                packageName = "podcast"
            }
        }
    }
}
