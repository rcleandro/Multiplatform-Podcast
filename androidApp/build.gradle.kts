plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "br.com.carvalho.podcast"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "br.com.carvalho.podcast"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
        // Release code, signed with the debug key so it installs anywhere; only the Macrobenchmark (:benchmark) uses it.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
    sourceSets["main"].res.directories.add("src/main/res")
    sourceSets["main"].java.directories.add("src/main/java")

    lint {
        // Existing findings (Android Auto voice search, exported media service…) are tracked in the roadmap;
        // only new ones fail the build.
        baseline = file("lint-baseline.xml")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core:observability"))
    implementation(project(":domain"))
    implementation(project(":core:player"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.window)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.media3.session)
    implementation(libs.coil.compose)
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    testImplementation(libs.junit)
    testImplementation(libs.glance.appwidget.testing)
    testImplementation(libs.robolectric)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.appcompat)
    implementation(libs.decompose)
    implementation(libs.koin.android)
}
