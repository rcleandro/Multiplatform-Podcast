import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Adds the Apple Watch (ADR 0016) to a `podcast.kmp.library` module the watch app uses: the data layer and `:watch`,
 * not the features, whose Compose resources have no watchOS. Code for both Apple platforms goes in `appleMain`; what
 * only the iPhone has (UIKit) stays in `iosMain`, with the watch's own in `watchosMain`.
 */
class KmpWatchConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.configure<KotlinMultiplatformExtension> {
            // arm64_32 (Series 4 to 8, SE), arm64 (Series 9 on, Ultra 2) and the simulator.
            watchosArm64()
            watchosDeviceArm64()
            watchosSimulatorArm64()
        }
    }
}
