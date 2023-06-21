plugins {
    id("com.mineinabyss.conventions.kotlin.jvm")
    id("com.mineinabyss.conventions.papermc")
    id("com.mineinabyss.conventions.copyjar")
    id("com.mineinabyss.conventions.publication")
    id("com.mineinabyss.conventions.autoversion")
    id("xyz.jpenilla.run-paper") version "2.0.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("net.minecrell.plugin-yml.bukkit") version "0.5.3"
    id("io.papermc.paperweight.userdev") version "1.5.5"
}

allprojects {
    apply(plugin = "java")

    version = rootProject.version

    repositories {
        maven("https://repo.dmulloy2.net/nexus/repository/public/")//ProtocolLib
    }

    dependencies {
        compileOnly(kotlin("stdlib-jdk8"))
    }
}

dependencies {
    // MineInAbyss platform
    compileOnly(libs.kotlinx.serialization.json)
    compileOnly(libs.kotlinx.serialization.kaml)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.minecraft.mccoroutine)

    // Shaded
    implementation(libs.bundles.idofront.core)
    paperweight.paperDevBundle("1.19.2-R0.1-SNAPSHOT") //NMS
    implementation(project(path = ":core"))
    implementation(project(path = ":v1_19_R1", configuration = "reobf"))
    implementation(project(path = ":v1_19_R2", configuration = "reobf"))
    implementation(project(path = ":v1_19_R3", configuration = "reobf"))
    implementation(project(path = ":v1_20_R1", configuration = "reobf"))
}

tasks {

    assemble {
        dependsOn(reobfJar)
    }

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
        minecraftVersion("1.20.1")
    }

    shadowJar {
        dependsOn(":v1_19_R1:reobfJar")
        dependsOn(":v1_19_R2:reobfJar")
        dependsOn(":v1_19_R3:reobfJar")
        dependsOn(":v1_20_R1:reobfJar")
        archiveFileName.set("Emojy.jar")
        //archiveFile.get().asFile.copyTo(layout.projectDirectory.file("run/plugins/ModernLightApi.jar").asFile, true)
    }

    build {
        dependsOn(shadowJar)
    }
}

bukkit {
    name = "Emojy"
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "com.mineinabyss.emojy.EmojyPlugin"
    apiVersion = "1.19"
    authors = listOf("boy0000")
    commands.register("emojy")
    description = "Custom emote plugin"
}
