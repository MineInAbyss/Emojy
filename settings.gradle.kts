rootProject.name = "emojy"

pluginManagement {
    val idofrontVersion: String by settings

    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://repo.mineinabyss.com/releases")
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenLocal()
    }

    resolutionStrategy.eachPlugin {
        if (requested.id.id.startsWith("com.mineinabyss.conventions"))
            useVersion(idofrontVersion)
    }
}

dependencyResolutionManagement {
    val idofrontVersion: String by settings

    repositories {
        maven("https://repo.mineinabyss.com/releases")
        mavenLocal()
    }

    versionCatalogs {
        create("libs"){
            from("com.mineinabyss:catalog:$idofrontVersion")
        }
        create("emojyLibs").from(files("gradle/emojyLibs.toml"))
    }
}

include("core", "v1_19_R1", "v1_19_R2", "v1_19_R3", "v1_20_R1", "v1_20_R2", "v1_20_R3")
