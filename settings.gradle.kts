pluginManagement {
    val kotlinVersion: String by settings
    val idofrontVersion: String by settings
    val composeVersion: String by settings

    repositories {
        gradlePluginPortal()
        maven("https://repo.mineinabyss.com/releases")
        maven("https://repo.papermc.io/repository/maven-public/")
        google()
    }

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jetbrains.dokka") version kotlinVersion
        id("org.jetbrains.compose") version composeVersion
    }

    resolutionStrategy.eachPlugin {
        if (requested.id.id.startsWith("com.mineinabyss.conventions")) useVersion(idofrontVersion)
    }
}

rootProject.name = "emojy"

dependencyResolutionManagement {
    val idofrontVersion: String by settings

    repositories {
        maven("https://repo.mineinabyss.com/releases")
    }

    versionCatalogs {
        create("libs").from("com.mineinabyss:catalog:$idofrontVersion")
        create("emojyLibs").from(files("gradle/emojyLibs.toml"))
    }
}
