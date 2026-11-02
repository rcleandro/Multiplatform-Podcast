plugins {
    alias(libs.plugins.podcast.kmp.library)
    alias(libs.plugins.podcast.kmp.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.podcast.app.version)
}

// The modules the SwiftUI screens use directly (ADR 0015): view models, their states and the domain models.
val swiftModules = listOf(
    ":core:common",
    ":domain",
    ":core:ui",
    ":feature:library",
    ":feature:podcast",
    ":feature:episode",
    ":feature:search",
    ":feature:player",
    ":feature:discover",
    ":feature:settings",
)

kotlin {
    android {
        namespace = "br.com.carvalho.podcast.shared"
        withHostTest { isIncludeAndroidResources = true }
    }

    cocoapods {
        version = appVersion.name
        summary = "Shared module for Podcast app"
        homepage = "https://github.com/rcleandro/Multiplatform-Podcast"
        ios.deploymentTarget = "18.0"
        framework {
            baseName = "Shared"
            isStatic = true
            linkerOpts("-ObjC")
            // What the SwiftUI screens read (ADR 0015): every public type of these modules reaches Swift under its
            // own name. Without it, a type only seen as a generic argument (a view model's UI state) is left out.
            swiftModules.forEach { export(project(it)) }
        }
        // The Firebase Kotlin SDK (dev.gitlive) brings its own bindings of these three pods; generating them again
        // binds each Objective-C class twice, which only the release link rejects ("FIROptions is already bound").
        pod("FirebaseAnalytics") {
            version = "~> 11.0"
            linkOnly = true
        }
        pod("FirebaseCrashlytics") {
            version = "~> 11.0"
            linkOnly = true
        }
        pod("FirebaseCore") {
            version = "~> 11.0"
            linkOnly = true
        }
        pod("FirebaseInstallations") {
            version = "~> 11.0"
        }
        pod("GoogleUtilities") {
            version = "~> 8.0"
        }
        pod("nanopb") {
            version = "~> 3.0"
        }
        pod("PromisesObjC") {
            version = "~> 2.0"
            moduleName = "FBLPromises"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":domain"))
            implementation(project(":data"))
            implementation(project(":feature:library"))
            implementation(project(":feature:podcast"))
            implementation(project(":feature:episode"))
            implementation(project(":feature:search"))
            implementation(project(":feature:player"))
            implementation(project(":feature:discover"))
            implementation(project(":feature:settings"))
            implementation(project(":core:database"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:network"))
            implementation(project(":core:player"))
            implementation(project(":core:ui"))
            implementation(libs.multiplatform.settings.no.arg)
            implementation(project(":core:observability"))

            // Compose
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.components.resources)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)

            // Adaptive
            implementation(libs.material3.adaptive)
            implementation(libs.material3.adaptive.layout)
            implementation(libs.material3.adaptive.navigation)
            implementation(libs.material3.adaptive.navigation.suite)

            // Room 3
            implementation(libs.room3.runtime)
            api(libs.room3.paging)


            // Paging
            implementation(libs.paging.common)
            implementation(libs.paging.compose)

            // Ktor 3
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.logging)
            implementation(libs.ktor.encoding)

            // Koin 4
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Kotlinx
            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            // Okio
            implementation(libs.okio)

            // Decompose
            implementation(libs.decompose)
            implementation(libs.decompose.compose)

            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodel)

            // Imagem
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // Logging
            implementation(libs.kermit)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.androidx.work.runtime)
            implementation(libs.play.services.wearable)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(project.dependencies.platform(libs.firebase.bom))
            // The player's video (19.10).
            implementation(libs.media3.ui.compose)
            // Brazil's age signal for the ECA Digital (26.2, ADR 0012).
            implementation(libs.play.age.signals)
        }

        // Firebase implementations shared by Android and iOS; Desktop and Web only log.
        val firebaseMain = create("firebaseMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.firebase.common)
                implementation(libs.firebase.analytics)
                implementation(libs.firebase.crashlytics)
            }
        }
        androidMain.get().dependsOn(firebaseMain)
        iosMain.get().dependsOn(firebaseMain)
        iosMain.dependencies { swiftModules.forEach { api(project(it)) } }

        // Library sync through a folder (19.12) where the platform keeps one: Android and Desktop.
        val folderSyncMain = create("folderSyncMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.filekit.core)
            }
        }
        androidMain.get().dependsOn(folderSyncMain)

        val desktopMain = getByName("desktopMain") {
            dependsOn(folderSyncMain)
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(compose.desktop.currentOs)
            }
        }

        wasmJsMain.dependencies {
            implementation(libs.okio.fakefilesystem)
        }

        commonTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
            implementation(libs.ui.test)
            implementation(libs.okio.fakefilesystem)
        }

        val jvmCommonTest = create("jvmCommonTest") {
            dependsOn(commonTest.get())
            dependencies {
                implementation(libs.mockk)
                implementation(libs.androidx.sqlite.bundled.jvm)
                implementation(libs.koin.test)
                implementation(libs.multiplatform.settings.test)
            }
        }

        val androidHostTest = getByName("androidHostTest") {
            dependsOn(jvmCommonTest)
            dependencies {
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.androidx.work.testing)
            }
        }

        val desktopTest = getByName("desktopTest") {
            dependsOn(jvmCommonTest)
        }
    }
}

// ponytail: the Kotlin CocoaPods plugin only raises pod deployment targets to iOS 12 (KT-57741), but Xcode 26+
// rejects anything below 15, which breaks the Android Studio sync and the iOS build. Patch the generated Podfile
// until the plugin raises it or Firebase 12 (minimum iOS 15) replaces the 11.x pods.
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.PodGenTask>().configureEach {
    doLast {
        val file = podfile.get()
        file.writeText(
            file.readText()
                .replace(
                    "deployment_target_major < 12 || (deployment_target_major == 12",
                    "deployment_target_major < 15 || (deployment_target_major == 15"
                )
                .replace("\"#{12}.#{0}\"", "\"#{15}.#{0}\"")
        )
    }
}

// The iOS app's texts, generated from the composeResources (ADR 0015): `generateIosStrings` after changing a text;
// `checkIosStrings` (run by `check` and the pre-commit hook) fails when the catalog was not generated again.
iosStrings(
    resources = listOf("core/ui", "core/designsystem").map { rootProject.file("$it/src/commonMain/composeResources") },
    catalog = rootProject.file("iosApp/iosApp/Localizable.xcstrings"),
)
