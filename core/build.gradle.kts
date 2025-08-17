plugins {
    id("maven-publish")
    id("com.mineinabyss.conventions.kotlin.jvm")
    id("com.mineinabyss.conventions.papermc")
    id("com.mineinabyss.conventions.autoversion")
    alias(idofrontLibs.plugins.kotlinx.serialization)
}

repositories {
    gradlePluginPortal()
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.mineinabyss.com/snapshots")
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
    google()
}

dependencies {
    // MineInAbyss platform
    compileOnly(idofrontLibs.bundles.idofront.core)
    compileOnly(idofrontLibs.kotlinx.serialization.json)
    compileOnly(idofrontLibs.kotlinx.serialization.kaml)
    compileOnly(idofrontLibs.kotlinx.coroutines)
    compileOnly(idofrontLibs.minecraft.mccoroutine)

    compileOnly(idofrontLibs.creative.api)
    compileOnly(idofrontLibs.creative.serializer.minecraft)

    // Shaded
    implementation("com.aaaaahhhhh.bananapuncher714:GifConverter:1.0")
}

publishing {
    repositories {
        maven {
            val repo = "https://repo.mineinabyss.com/"
            val isSnapshot = System.getenv("IS_SNAPSHOT") == "true"
            val url = if (isSnapshot) repo + "snapshots" else repo + "releases"
            setUrl(url)
            credentials {
                username = project.findProperty("mineinabyssMavenUsername") as String?
                password = project.findProperty("mineinabyssMavenPassword") as String?
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = rootProject.group.toString()
            artifactId = rootProject.name
            version = rootProject.version.toString()
        }
    }
}
