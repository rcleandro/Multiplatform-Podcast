import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.decompose)
            implementation(libs.runtime)
            implementation(libs.compose.ui)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.koin.wasm)
            implementation(libs.room3.runtime)
            implementation(libs.sqlite.web)
        }
    }
}

tasks.register<Copy>("copySqliteWorker") {
    dependsOn("wasmJsProcessResources")

    from(project.configurations.getByName("wasmJsRuntimeClasspath")
        .resolvedConfiguration
        .resolvedArtifacts
        .filter { it.name.contains("sqlite-web-wasm-js") || it.name.contains("sqlite-web") }
        .map { zipTree(it.file) }
    )
    include("**/sqlite-worker.js", "**/sqlite-worker.wasm")
    into(layout.buildDirectory.dir("processedResources/wasmJs/main"))
    includeEmptyDirs = false
}

tasks.named("wasmJsDevelopmentExecutableCompileSync") {
    dependsOn("copySqliteWorker")
}

tasks.named("wasmJsBrowserDevelopmentRun") {
    dependsOn("copySqliteWorker")
}

tasks.named("wasmJsBrowserProductionWebpack") {
    dependsOn("copySqliteWorker")
}
