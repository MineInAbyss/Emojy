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
    implementation(project(":paper"))
}

idofront {
    minecraftVersion = "26.3.build.18-alpha"
}
