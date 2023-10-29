import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("com.mineinabyss.conventions.kotlin.jvm")
    id("com.mineinabyss.conventions.papermc")
    id("com.mineinabyss.conventions.copyjar")
    id("com.mineinabyss.conventions.publication")
    id("com.mineinabyss.conventions.autoversion")
    id("xyz.jpenilla.run-paper") version "2.0.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("io.papermc.paperweight.userdev") version "1.5.8"
}

allprojects {
    apply(plugin = "java")

    version = rootProject.version

    dependencies {
        compileOnly(kotlin("stdlib-jdk8"))
    }
}

dependencies {
    // MineInAbyss platform
    compileOnly(libs.bundles.idofront.core)
    compileOnly(libs.kotlinx.serialization.json)
    compileOnly(libs.kotlinx.serialization.kaml)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.minecraft.mccoroutine)

    // Shaded
    paperweight.paperDevBundle("1.20.2-R0.1-SNAPSHOT") //NMS
    implementation(project(path = ":core"))
    implementation(project(path = ":v1_19_R1", configuration = "reobf"))
    implementation(project(path = ":v1_19_R2", configuration = "reobf"))
    implementation(project(path = ":v1_19_R3", configuration = "reobf"))
    implementation(project(path = ":v1_20_R1", configuration = "reobf"))
    implementation(project(path = ":v1_20_R2", configuration = "reobf"))
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
        minecraftVersion("1.20.2")
    }

    shadowJar {
        dependsOn(":v1_19_R1:reobfJar")
        dependsOn(":v1_19_R2:reobfJar")
        dependsOn(":v1_19_R3:reobfJar")
        dependsOn(":v1_20_R1:reobfJar")
        dependsOn(":v1_20_R2:reobfJar")
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
    apiVersion = "1.19"

    serverDependencies {
        register("Idofront") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }
}
