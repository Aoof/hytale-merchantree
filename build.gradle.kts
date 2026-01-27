plugins {
    `maven-publish`
    id("java")
}

// =============================================================================
// Project Configuration
// =============================================================================

group = "ca.aoof"
version = "0.1.0"

val javaVersion = 25
val hytaleServerVersion = "2026.01.27-734d39026"

// =============================================================================
// Platform Detection & Paths
// =============================================================================

val osName: String = System.getProperty("os.name", "").lowercase()
val userHome: String = System.getProperty("user.home") ?: System.getenv("HOME") ?: "."
val isMacOs = osName.contains("mac") || osName.contains("darwin")
val isWindows = osName.contains("win")

val appData: File = when {
    isMacOs -> File(userHome, "Library/Application Support")
    isWindows -> System.getenv("APPDATA")?.let { File(it) } ?: File(userHome, "AppData")
    else -> File(userHome, ".var/app/com.hypixel.HytaleLauncher/data")
}

val hytaleGameDir: File = appData.resolve("Hytale/install/release/package/game/latest")
val hytaleAssets: File = hytaleGameDir.resolve("Assets.zip")
val hytaleServerSource: File = hytaleGameDir.resolve("Server/HytaleServer.jar")

val librariesDir: File = when {
    isMacOs -> File(userHome, "Documents/Coding/Hytale/Libraries")
    isWindows -> {
        val primary = File("D:/Hytale/HytaleMods/Libraries")
        if (primary.exists()) primary else File(userHome, "Documents/Libraries")
    }
    else -> File(userHome, "Documents/Libraries")
}

// Server run directory (where server files and configs will be generated)
val serverRunDir: File = layout.projectDirectory.dir("run").asFile

// Tool and artifact paths (can be overridden via gradle.properties)
val vineflowerJar: String = findProperty("vineflowerJar")?.toString()
    ?: File(librariesDir, "vineflower-1.11.2.jar").absolutePath
val serverJar: String = findProperty("serverJar")?.toString()
    ?: File(librariesDir, "HytaleServer.jar").absolutePath
val decompileOutputDir: String = findProperty("outputDir")?.toString()
    ?: File(librariesDir, "DecompiledServer").absolutePath

// =============================================================================
// Repositories
// =============================================================================

repositories {
    mavenCentral()
    maven {
        name = "hytale-release"
        url = uri("https://maven.hytale.com/release")
    }
    maven {
        name = "hytale-pre-release"
        url = uri("https://maven.hytale.com/pre-release")
    }
}

// =============================================================================
// Dependencies
// =============================================================================

dependencies {
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.jspecify)
    compileOnly("com.hypixel.hytale:Server:$hytaleServerVersion")

    if (hytaleAssets.exists()) {
        compileOnly(files(hytaleAssets))
    } else {
        logger.warn("⚠️ Hytale Assets.zip not found at: ${hytaleAssets.absolutePath}")
    }
}

// =============================================================================
// Java Configuration
// =============================================================================

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }

    withSourcesJar()
}

// =============================================================================
// Tasks - Resource Processing
// =============================================================================

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "plugin_group" to findProperty("plugin_group"),
        "plugin_maven_group" to project.group,
        "plugin_name" to project.name,
        "plugin_version" to project.version,
        "server_version" to findProperty("server_version"),

        "plugin_description" to findProperty("plugin_description"),
        "plugin_website" to findProperty("plugin_website"),

        "plugin_main_entrypoint" to findProperty("plugin_main_entrypoint"),
        "plugin_author" to findProperty("plugin_author")
    )

    filesMatching("manifest.json") {
        expand(replaceProperties)
    }
}

// =============================================================================
// Tasks - JAR Configuration
// =============================================================================

tasks.withType<Jar> {
    manifest {
        attributes["Specification-Title"] = rootProject.name
        attributes["Specification-Version"] = version
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] =
            providers.environmentVariable("COMMIT_SHA_SHORT")
                .map { "${version}-${it}" }
                .getOrElse(version.toString())
    }
}

// =============================================================================
// Publishing
// =============================================================================

publishing {
    repositories {
        // This is where you put repositories that you want to publish to.
        // Do NOT put repositories for your dependencies here.
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

// =============================================================================
// Tasks - Hytale Server Management
// =============================================================================

/**
 * Copies HytaleServer.jar from the game installation to the libraries directory.
 * Run this manually when you need to update your local server jar.
 */
val copyServerFromGame by tasks.registering {
    group = "hytale"
    description = "Copies HytaleServer.jar from the game installation to the libraries directory"

    // Only run if the source file exists and is newer than destination
    onlyIf { hytaleServerSource.exists() }

    doLast {
        val dest = File(serverJar)
        dest.parentFile?.mkdirs()
        hytaleServerSource.copyTo(dest, overwrite = true)
        logger.lifecycle("✅ Copied HytaleServer.jar from ${hytaleServerSource.absolutePath} to ${dest.absolutePath}")
    }
}

/**
 * Decompiles the Hytale server using Vineflower.
 * Requires vineflower jar and server jar to be present in the libraries directory.
 */
val decompileServer by tasks.registering(Exec::class) {
    group = "hytale"
    description = "Decompiles the Hytale server using Vineflower"

    inputs.file(serverJar)
    outputs.dir(decompileOutputDir)

    doFirst {
        require(File(serverJar).exists()) {
            "Server jar not found at: $serverJar\nRun 'copyServerFromGame' task first or download manually."
        }
        require(File(vineflowerJar).exists()) {
            "Vineflower jar not found at: $vineflowerJar\nPlease download vineflower and place it in the libraries directory."
        }

        // Clean the output directory before decompiling
        val outputDir = file(decompileOutputDir)
        if (outputDir.exists()) {
            logger.lifecycle("🗑️ Cleaning existing decompiled files at: ${outputDir.absolutePath}")
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()
    }

    commandLine(
        "java", "-Xms2G", "-Xmx12G",
        "-jar", vineflowerJar,
        "-s", "--only=com/hypixel",
        serverJar, decompileOutputDir
    )
}

/**
 * Runs the Hytale server from the libraries directory.
 * Server files (configs, worlds, etc.) will be generated in the run/ directory.
 */
val runServer by tasks.registering(JavaExec::class) {
    group = "hytale"
    description = "Runs the Hytale server (files generated in run/ directory)"

    dependsOn(tasks.named("build"))

    // Enable interactive console input
    standardInput = System.`in`

    // Ensure the run directory exists
    doFirst {
        serverRunDir.mkdirs()
        val modsDir = serverRunDir.resolve("mods")
        modsDir.mkdirs()
        val modJar = layout.buildDirectory.file("libs/${project.name}-${project.version}.jar").get().asFile
        require(modJar.exists()) {
            "Mod jar not found at: ${modJar.absolutePath}\nMake sure the build task completed successfully."
        }
        modJar.copyTo(modsDir.resolve(modJar.name), overwrite = true)
        logger.lifecycle("📦 Copied mod jar to: ${modsDir.resolve(modJar.name).absolutePath}")
        require(File(serverJar).exists()) {
            "Server jar not found at: $serverJar\nRun 'copyServerFromGame' task first or download manually."
        }
        require(hytaleAssets.exists()) {
            "Assets.zip not found at: ${hytaleAssets.absolutePath}"
        }
        logger.lifecycle("🚀 Starting Hytale server from: $serverJar")
        logger.lifecycle("📂 Working directory: ${serverRunDir.absolutePath}")
    }

    workingDir = serverRunDir
    classpath = files(serverJar)
    args(
        "--assets", hytaleAssets.absolutePath,
        "--disable-sentry"
    )
}