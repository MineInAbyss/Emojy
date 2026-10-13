plugins {
    alias(miaLibs.plugins.mia.kotlin.jvm)
    id("com.gradleup.shadow")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    // velocity-proxy is only published here, and lags behind velocity-api
    maven("https://repo.opencollab.dev/maven-snapshots")
    maven("https://repo.william278.net/releases")
}

dependencies {
    compileOnly(libs.velocity.api)
    compileOnly(libs.velocity.proxy) { isTransitive = false }
    compileOnly(libs.velocitab) { isTransitive = false }
    compileOnly(libs.netty.transport)

    implementation(project(":common"))
    implementation(miaLibs.idofront.fonts)
}

tasks {
    processResources {
        val version = project.version.toString()
        inputs.property("version", version)
        filesMatching("velocity-plugin.json") { expand("version" to version) }
    }

    shadowJar {
        archiveFileName.set("Emojy-Velocity.jar")
    }

    build { dependsOn(shadowJar) }
}
