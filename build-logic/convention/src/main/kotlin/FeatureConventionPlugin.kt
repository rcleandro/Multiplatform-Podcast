import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A feature module: Compose UI plus what every screen uses. Only the modules ADR 0003 allows are added here;
 * the module still declares `kotlin { android { namespace = "…" } }`.
 * `checkModuleDependencies` (run by `check`) fails if the module depends on another feature or on a data layer module.
 * Screens get Roborazzi snapshots in `androidHostTest` (Robolectric), recorded on Linux by the "Record snapshots"
 * workflow into the module's `snapshots/` folder and verified in CI (17.4).
 */
class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("podcast.kmp.library")
        pluginManager.apply("podcast.kmp.compose")
        pluginManager.apply("io.github.takahirom.roborazzi")
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
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                withHostTest { isIncludeAndroidResources = true }
            }
            sourceSets.getByName("androidHostTest").dependencies {
                listOf(
                    "junit", "robolectric", "roborazzi", "roborazzi-compose", "androidx-compose-ui-test-junit4",
                    "androidx-compose-ui-test-manifest",
                ).forEach { implementation(lib(it)) }
            }
        }

        val forbidden = provider {
            configurations.flatMap { configuration ->
                configuration.dependencies.withType(ProjectDependency::class.java)
                    .map { it.path }
                    // A test source set depends on its own module, which is not another feature.
                    .filter { it != target.path && (it.startsWith(":feature:") || it in DATA_LAYER_MODULES) }
                    .map { "${configuration.name} -> $it" }
            }.distinct()
        }
        val checkModuleDependencies = tasks.register("checkModuleDependencies") {
            group = "verification"
            description = "Fails if this feature depends on another feature or on a data layer module (ADR 0003)."
            val modulePath = target.path
            inputs.property("forbidden", forbidden)
            doLast {
                val violations = forbidden.get()
                if (violations.isNotEmpty()) {
                    throw GradleException(
                        "$modulePath breaks ADR 0003, features may not depend on:\n" + violations.joinToString("\n")
                    )
                }
            }
        }
        tasks.named("check") { dependsOn(checkModuleDependencies) }
    }

    private companion object {
        val DATA_LAYER_MODULES = setOf(":data", ":core:database", ":core:network", ":core:player")
    }
}
