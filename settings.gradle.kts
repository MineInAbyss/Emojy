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
            version("minecraft-server", "26.2.build.123-stable")
            version("java", "25")
            version("creative", "1.14.3")
            version("idofront", "2.0")
        }
    }
}

// legacy nms modules cannot build against a java 25 core, and 26.2 only needs v1_21_R10
include("core", "v1_21_R10")
