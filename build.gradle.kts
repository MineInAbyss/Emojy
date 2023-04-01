plugins {
    id("com.mineinabyss.conventions.kotlin")
    id("com.mineinabyss.conventions.papermc")
    id("com.mineinabyss.conventions.copyjar")
    id("com.mineinabyss.conventions.publication")
    id("com.mineinabyss.conventions.autoversion")
    id("xyz.jpenilla.run-paper") version "2.0.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
    kotlin("plugin.serialization")
}

dependencies {
    // MineInAbyss platform
    compileOnly(libs.kotlinx.serialization.json)
    compileOnly(libs.kotlinx.serialization.kaml)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.minecraft.mccoroutine)
    compileOnly(libs.koin.core)

    // Shaded
    implementation(libs.bundles.idofront.core)
    implementation(project(path = ":core"))
    implementation(project(path = ":v1_19_R1", configuration = "reobf"))
    implementation(project(path = ":v1_19_R2", configuration = "reobf"))
}

tasks {

    /*assemble {
        dependsOn(reobfJar)
    }*/

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }

    runServer {
        minecraftVersion("1.19.4")
    }

    shadowJar {
        dependsOn(":v1_19_R1:reobfJar")
        dependsOn(":v1_19_R2:reobfJar")
        dependsOn(":v1_19_R3:reobfJar")
        archiveFileName.set("Emojy.jar")
        //archiveFile.get().asFile.copyTo(layout.projectDirectory.file("run/plugins/ModernLightApi.jar").asFile, true)
    }

    build {
        dependsOn(shadowJar)
    }
}

bukkit {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "com.mineinabyss.emojy.EmojyPlugin"
    apiVersion = "1.19"
    authors = listOf("boy0000")
    commands.register("emojy")
}
