import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    alias(miaLibs.plugins.mia.kotlin.jvm)
    alias(miaLibs.plugins.mia.nms)
}

paperweight.reobfArtifactConfiguration.set(ReobfArtifactConfiguration.MOJANG_PRODUCTION)

dependencies {
    compileOnly(miaLibs.kotlinx.serialization.json)
    compileOnly(miaLibs.kotlinx.serialization.kaml)
    compileOnly(miaLibs.kotlinx.coroutines)
    compileOnly(miaLibs.minecraft.mccoroutine)

    implementation(miaLibs.bundles.idofront.core)
    implementation(project(":core"))
}

idofront {
    minecraftVersion = "26.1.2.build.74-stable"
}
