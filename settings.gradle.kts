pluginManagement {
    repositories {
        gradlePluginPortal()
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
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Merchantree"
