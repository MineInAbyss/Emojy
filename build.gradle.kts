import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    alias(miaLibs.plugins.mia.kotlin.jvm)
    alias(miaLibs.plugins.mia.papermc)
    alias(miaLibs.plugins.mia.copyjar)
    alias(miaLibs.plugins.mia.nms)
    alias(miaLibs.plugins.mia.docs)
}

allprojects {
    apply(plugin = "java")

    version = rootProject.version

    repositories {
        maven("https://repo.mineinabyss.com/releases")
        maven("https://repo.mineinabyss.com/snapshots")
        maven("https://repo.nexomc.com/releases")
        maven("https://repo.nexomc.com/snapshots")
        mavenLocal()
    }

    dependencies {
        implementation("com.jeff-media:MorePersistentDataTypes:2.4.0")
    }
}

repositories {
    mavenLocal()
}

dependencies {
    // MineInAbyss platform
    compileOnly(miaLibs.bundles.idofront.core)
    compileOnly(miaLibs.kotlinx.serialization.json)
    compileOnly(miaLibs.kotlinx.serialization.kaml)
    compileOnly(miaLibs.kotlinx.coroutines)
    compileOnly(miaLibs.minecraft.mccoroutine)

    // Shaded
    paperweight.paperDevBundle("26.2.build.123-stable") //NMS
    implementation(project(path = ":core"))
    implementation(project(path = ":v1_21_R10"))
}

tasks {

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }

//    runServer {
//        minecraftVersion("1.21")
//    }

    shadowJar {
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
    apiVersion = "1.21"

    bootstrapper = "com.mineinabyss.emojy.EmojyBootstrap"

    bootstrapDependencies {
        register("Idofront") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }

    serverDependencies {
        register("Idofront") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }
}
