import org.gradle.api.GradleException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {
    @Test
    fun theCodeGrowsWithEveryReleaseAndTheWatchGetsItsOwn() {
        val versions = listOf("1.0.0", "1.0.1", "1.0.99", "1.1.0", "1.99.0", "2.0.0").map(AppVersion::of)

        assertEquals(100_000, versions.first().code)
        assertEquals(100_001, versions.first().watchCode)
        versions.zipWithNext().forEach { (older, newer) -> assertTrue(newer.code > older.watchCode) }
    }

    @Test
    fun aNameThatIsNotMajorMinorPatchStopsTheBuild() {
        listOf("1.0", "1.0.0-beta", "1.100.0", "v1.0.0").forEach { name ->
            assertThrows(GradleException::class.java) { AppVersion.of(name) }
        }
    }
}
