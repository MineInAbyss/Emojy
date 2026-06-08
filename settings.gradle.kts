rootProject.name = "emojy"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://repo.mineinabyss.com/releases")
        maven("https://repo.mineinabyss.com/snapshots")
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenLocal()
    }
}

dependencyResolutionManagement {
    val miaLibs: String by settings

    repositories {
        maven("https://repo.mineinabyss.com/releases")
        maven("https://repo.mineinabyss.com/snapshots")
        mavenLocal()
    }

    versionCatalogs {
        create("miaLibs"){
            from("com.mineinabyss:catalog:$miaLibs")
        }
    }
}

include("core", "v1_21_R1", "v1_21_R2", "v1_21_R3", "v1_21_R4", "v1_21_R6", "v1_21_R8", "v1_21_R10")
