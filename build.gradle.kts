plugins {
    id("com.mineinabyss.conventions.kotlin")
    id("com.mineinabyss.conventions.papermc")
    id("com.mineinabyss.conventions.copyjar")
    id("com.mineinabyss.conventions.nms")
    id("com.mineinabyss.conventions.publication")
    id("com.mineinabyss.conventions.autoversion")
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
    implementation(libs.idofront.nms)
}
