import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    id("com.mineinabyss.conventions.kotlin.jvm")
    id("com.mineinabyss.conventions.autoversion")
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

repositories {
    gradlePluginPortal()
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.mineinabyss.com/snapshots")
    maven("https://repo.papermc.io/repository/maven-public/")
    google()
}

paperweight.reobfArtifactConfiguration.set(ReobfArtifactConfiguration.MOJANG_PRODUCTION)

dependencies {
    // MineInAbyss platform
    compileOnly(idofrontLibs.kotlinx.serialization.json)
    compileOnly(idofrontLibs.kotlinx.serialization.kaml)
    compileOnly(idofrontLibs.kotlinx.coroutines)
    compileOnly(idofrontLibs.minecraft.mccoroutine)

    // Shaded
    implementation(idofrontLibs.bundles.idofront.core)
    implementation(project(":core"))
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT") //NMS
}

tasks {

    build {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}
