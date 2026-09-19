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

include("core")

// Modules are named for the Minecraft version they are compiled against
listOf("v1_21_11", "v26_1_2", "v26_2", "v26_3").forEach(::registerNms)
fun registerNms(version: String) {
    include(":nms:$version")
    project(":nms:$version").projectDir = file("nms/$version")
}
