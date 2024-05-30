import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("com.mineinabyss.conventions.kotlin.jvm")
    id("com.mineinabyss.conventions.papermc")
    id("com.mineinabyss.conventions.copyjar")
    id("com.mineinabyss.conventions.publication")
    id("com.mineinabyss.conventions.autoversion")
    id("xyz.jpenilla.run-paper") version "2.0.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

paperweight.reobfArtifactConfiguration.set(ReobfArtifactConfiguration.MOJANG_PRODUCTION)

allprojects {
    apply(plugin = "java")

    version = rootProject.version

    repositories {
        maven("https://repo.mineinabyss.com/releases")
        maven("https://repo.mineinabyss.com/snapshots")
        maven("https://hub.jeff-media.com/nexus/repository/jeff-media-public/")
        mavenLocal()
    }

    dependencies {
        implementation("com.jeff_media:MorePersistentDataTypes:2.4.0")
    }
}

dependencies {
    // MineInAbyss platform
    compileOnly(idofrontLibs.bundles.idofront.core)
    compileOnly(idofrontLibs.kotlinx.serialization.json)
    compileOnly(idofrontLibs.kotlinx.serialization.kaml)
    compileOnly(idofrontLibs.kotlinx.coroutines)
    compileOnly(idofrontLibs.minecraft.mccoroutine)

    // Shaded
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT") //NMS
    implementation(project(path = ":core"))
    implementation(project(path = ":v1_20_R4"))
}

tasks {

    assemble {
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
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }

    runServer {
        minecraftVersion("1.20.6")
    }

    shadowJar {
        dependsOn(":v1_20_R4:reobfJar")

        relocate("com.jeff_media.morepersistentdatatypes", "com.mineinabyss.shaded.morepersistentdatatypes")

        archiveFileName.set("Emojy.jar")
    }

    build.get().dependsOn(shadowJar)
}

paper {
    main = "com.mineinabyss.emojy.EmojyPlugin"
    name = "Emojy"
    prefix = "Emojy"
    val version: String by project
    this.version = version
    authors = listOf("boy0000")
    apiVersion = "1.20"

    serverDependencies {
        register("Idofront") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }
}
