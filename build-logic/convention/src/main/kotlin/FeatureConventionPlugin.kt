import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A feature module: Compose UI plus what every screen uses. Only the modules ADR 0003 allows are added here;
 * the module still declares `kotlin { android { namespace = "…" } }`.
 */
class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("podcast.kmp.library")
        pluginManager.apply("podcast.kmp.compose")
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        fun lib(alias: String) = libs.findLibrary(alias).get()

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":domain"))
                implementation(project(":core:common"))
                implementation(project(":core:observability"))
                implementation(project(":core:designsystem"))
                implementation(project(":core:ui"))
                listOf(
                    "material3-adaptive", "material3-adaptive-layout", "composeIconsExtended", "compose-uiToolingPreview",
                    "components-resources", "koin-compose", "koin-compose-viewmodel", "androidx-lifecycle-viewmodel",
                    "paging-common", "paging-compose"
                ).forEach { implementation(lib(it)) }
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(project(":core:testing"))
                listOf("kotlin-test", "kotlinx-coroutines-test", "turbine", "ui-test").forEach { implementation(lib(it)) }
            }
            sourceSets.getByName("desktopTest").dependencies {
                implementation(ComposePlugin.DesktopDependencies.currentOs)
            }
        }
    }
}
