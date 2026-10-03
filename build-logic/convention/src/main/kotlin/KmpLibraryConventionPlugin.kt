import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Kotlin Multiplatform library for every platform the app ships on: Android, Desktop (JVM), Wasm and iOS.
 * The module still declares `kotlin { android { namespace = "…" } }`; SDK levels and resources come from here.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        fun version(alias: String) = libs.findVersion(alias).get().requiredVersion.toInt()

        extensions.configure<KotlinMultiplatformExtension> {
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                compileSdk = version("android-compileSdk")
                minSdk = version("android-minSdk")
                // Packages Compose resources (strings, fonts) into the APK without manual copy tasks.
                androidResources.enable = true
            }
            jvm("desktop")
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs { browser() }
            iosArm64()
            iosSimulatorArm64()
            applyDefaultHierarchyTemplate()
        }
    }
}
