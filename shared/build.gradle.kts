plugins {
    alias(libs.plugins.podcast.kmp.library)
    alias(libs.plugins.podcast.kmp.compose)
    alias(libs.plugins.kotlin.serialization)
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

    // The iOS app links Shared through embedAndSignAppleFrameworkForXcode and Firebase through Swift Package Manager
    // (25.15), as the watch does with WatchShared.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // What the SwiftUI screens read (ADR 0015): every public type of these modules reaches Swift under its
            // own name. Without it, a type only seen as a generic argument (a view model's UI state) is left out.
            swiftModules.forEach { export(project(it)) }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            api(project(":core:di"))
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

            // Room 3
            implementation(libs.room3.runtime)
            api(libs.room3.paging)


            // Paging
            implementation(libs.paging.common)

            // Ktor 3
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.logging)
            implementation(libs.ktor.encoding)

            // Koin 4
            api(libs.koin.core)

            // Kotlinx
            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            // Okio
            implementation(libs.okio)

            // Decompose
            implementation(libs.decompose)

            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.koin.core.viewmodel)

            // Logging
            implementation(libs.kermit)
        }

        // The Compose app; iOS draws its screens in SwiftUI (ADR 0015).
        getByName("composeMain").dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.components.resources)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.material3.adaptive)
            implementation(libs.material3.adaptive.layout)
            implementation(libs.material3.adaptive.navigation)
            implementation(libs.material3.adaptive.navigation.suite)
            implementation(libs.paging.compose)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.decompose.compose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.androidx.work.runtime)
            implementation(libs.play.services.wearable)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(project.dependencies.platform(libs.firebase.bom))
            // iOS reaches Firebase from Swift instead (25.15).
            implementation(libs.firebase.common)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            // The player's video (19.10).
            implementation(libs.media3.ui.compose)
            // Brazil's age signal for the ECA Digital (26.2, ADR 0012).
            implementation(libs.play.age.signals)
        }

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

        // The web entry point starts the Compose app.
        getByName("webMain").dependsOn(getByName("composeMain"))
        wasmJsMain.dependencies {
            implementation(libs.okio.fakefilesystem)
        }

        commonTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
            implementation(libs.okio.fakefilesystem)
        }

        getByName("composeTest").dependencies {
            implementation(libs.ui.test)
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

// The iOS app's texts, generated from the composeResources (ADR 0015): `generateIosStrings` after changing a text;
// `checkIosStrings` (run by `check` and the pre-commit hook) fails when the catalog was not generated again.
iosStrings(
    resources = listOf("core/ui", "core/designsystem").map { rootProject.file("$it/src/commonMain/composeResources") },
    catalog = rootProject.file("iosApp/iosApp/Localizable.xcstrings"),
)
