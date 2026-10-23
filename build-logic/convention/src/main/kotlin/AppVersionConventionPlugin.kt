import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

/**
 * The app's version on every platform (20.2), from `version.properties` at the root. That file is also an xcconfig:
 * `iosApp/Configuration/Config.xcconfig` includes it, so the iOS app reads the same line.
 * Applying this plugin adds `appVersion` to the project.
 */
class AppVersionConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val file = target.rootProject.file("version.properties")
        val name = Properties().apply { file.reader().use(::load) }.getProperty("APP_VERSION")
            ?: throw GradleException("APP_VERSION is missing from $file")
        target.extensions.add("appVersion", AppVersion.of(name))
    }
}

/** [name] is `MAJOR.MINOR.PATCH`; [code] is the Android `versionCode`, which grows with it. */
data class AppVersion(val name: String, val code: Int) {
    /** The watch app ships under the same application ID, so the store needs its own, different code. */
    val watchCode: Int get() = code + WATCH

    companion object {
        private const val PART_LIMIT = 100
        private const val FORM_FACTORS = 10
        private const val WATCH = 1

        fun of(name: String): AppVersion {
            val parts = name.split(".").map { it.toIntOrNull() }
            if (parts.size != 3 || parts.any { it == null || it !in 0 until PART_LIMIT }) {
                throw GradleException("APP_VERSION must be MAJOR.MINOR.PATCH, each below $PART_LIMIT: $name")
            }
            val (major, minor, patch) = parts.map { it!! }
            return AppVersion(name, ((major * PART_LIMIT + minor) * PART_LIMIT + patch) * FORM_FACTORS)
        }
    }
}
