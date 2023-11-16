plugins {
    id("com.mineinabyss.conventions.kotlin.jvm")
    id("com.mineinabyss.conventions.papermc")
    id("com.mineinabyss.conventions.autoversion")
    alias(libs.plugins.kotlinx.serialization)
}

repositories {
    gradlePluginPortal()
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.unnamed.team/repository/unnamed-public/")
    google()
}

dependencies {
    // MineInAbyss platform
    compileOnly(libs.bundles.idofront.core)
    compileOnly(libs.kotlinx.serialization.json)
    compileOnly(libs.kotlinx.serialization.kaml)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.minecraft.mccoroutine)

    compileOnly(libs.creative.api)
    compileOnly(libs.creative.serializer.minecraft)

    // Shaded
    implementation("com.aaaaahhhhh.bananapuncher714:GifConverter:1.0")
}
