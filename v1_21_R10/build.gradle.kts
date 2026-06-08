import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    alias(miaLibs.plugins.mia.kotlin.jvm)
    alias(miaLibs.plugins.mia.nms)
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
    compileOnly(miaLibs.kotlinx.serialization.json)
    compileOnly(miaLibs.kotlinx.serialization.kaml)
    compileOnly(miaLibs.kotlinx.coroutines)
    compileOnly(miaLibs.minecraft.mccoroutine)

    // Shaded
    implementation(miaLibs.bundles.idofront.core)
    implementation(project(":core"))
}


idofront {
    minecraftVersion = "1.21.10-R0.1-SNAPSHOT"
}

tasks {

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
