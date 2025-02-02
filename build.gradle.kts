import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("com.mineinabyss.conventions.kotlin.jvm")
    id("com.mineinabyss.conventions.papermc")
    id("com.mineinabyss.conventions.copyjar")
    id("com.mineinabyss.conventions.publication")
    id("com.mineinabyss.conventions.autoversion")
    id("xyz.jpenilla.run-paper") version "2.3.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}

paperweight.reobfArtifactConfiguration.set(ReobfArtifactConfiguration.MOJANG_PRODUCTION)

allprojects {
    apply(plugin = "java")

    version = rootProject.version

    repositories {
        maven("https://repo.mineinabyss.com/releases")
        maven("https://repo.mineinabyss.com/snapshots")
        maven("https://repo.nexomc.com/snapshots")
        mavenLocal()
    }

    dependencies {
        implementation("com.jeff-media:MorePersistentDataTypes:2.4.0")
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
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT") //NMS
    implementation(project(path = ":core"))
    implementation(project(path = ":v1_20_R4"))
    implementation(project(path = ":v1_21_R1"))
    implementation(project(path = ":v1_21_R2"))
    implementation(project(path = ":v1_21_R3"))
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
        minecraftVersion("1.21")
    }

    shadowJar {
        dependsOn(":v1_20_R4:reobfJar")

        //relocate("com.jeff_media.morepersistentdatatypes", "com.mineinabyss.shaded.morepersistentdatatypes")

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
