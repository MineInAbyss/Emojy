rootProject.name = "emojy"

pluginManagement {
    val idofrontVersion: String by settings

    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://repo.mineinabyss.com/releases")
        maven("https://repo.mineinabyss.com/snapshots")
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
        maven("https://repo.mineinabyss.com/snapshots")
        mavenLocal()
    }

    versionCatalogs {
        create("idofrontLibs"){
            from("com.mineinabyss:catalog:$idofrontVersion")
        }
    }
}

include("core", "v1_21_R1", "v1_21_R2", "v1_21_R3", "v1_21_R4", "v1_21_R6")
