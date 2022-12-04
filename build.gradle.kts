plugins {
    id("com.mineinabyss.conventions.kotlin")
    id("com.mineinabyss.conventions.papermc")
    id("com.mineinabyss.conventions.copyjar")
    id("com.mineinabyss.conventions.nms")
    id("com.mineinabyss.conventions.publication")
    id("com.mineinabyss.conventions.autoversion")
    kotlin("plugin.serialization")
}

repositories {
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    // MineInAbyss platform
    compileOnly(libs.kotlinx.serialization.json)
    compileOnly(libs.kotlinx.serialization.kaml)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.minecraft.mccoroutine)
    compileOnly(libs.koin.core)
    compileOnly(libs.minecraft.plugin.protocollib)

    // Shaded
    implementation(libs.bundles.idofront.core)
    implementation(libs.idofront.nms)
}
