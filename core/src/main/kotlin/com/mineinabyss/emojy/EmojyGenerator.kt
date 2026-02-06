package com.mineinabyss.emojy

import com.mineinabyss.idofront.font.Space
import com.mineinabyss.idofront.font.Space.Companion.toNumber
import com.mineinabyss.idofront.resourcepacks.ResourcePacks
import com.mineinabyss.idofront.util.removeSuffix
import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.atlas.Atlas
import team.unnamed.creative.atlas.AtlasSource
import team.unnamed.creative.atlas.SingleAtlasSource
import team.unnamed.creative.base.Writable
import team.unnamed.creative.font.BitMapFontProvider
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.font.SpaceFontProvider
import team.unnamed.creative.metadata.overlays.OverlayEntry
import team.unnamed.creative.metadata.overlays.OverlaysMeta
import team.unnamed.creative.metadata.pack.PackFormat
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter
import team.unnamed.creative.texture.Texture

object EmojyGenerator {
    val gifFolder = emojy.plugin.dataFolder.resolve("gifs").apply { mkdirs() }
    private val emotesFolder = emojy.plugin.dataFolder.resolve("emotes").apply { mkdirs() }
    private val spaceProvider = FontProvider.space(Space.entries.asSequence().filterNot(Space.NULL::equals).associate { it.unicode to it.toNumber() })
    private val unknownTexture by lazy { ResourcePacks.vanillaResourcePack.texture(Key.key("gui/sprites/icon/unseen_notification.png")) }
    val resourcePack = ResourcePack.resourcePack()

    fun generateResourcePack() {
        ResourcePacks.clearPack(resourcePack)
        emojy.plugin.dataFolder.resolve("assets").deleteRecursively()

        val textureFiles = emotesFolder.walkTopDown().filter { it.isFile }.associateBy { it.name }
        val fontSpaceProvider = FontProvider.space().advance("\uE101", -1).build()
        emojy.emotes.forEach { emote ->
            resourcePack.font(emote.font)?.takeIf { emote.isMultiBitmap }?.also { font ->
                when {
                    // Add a -1 advance to the font for ease of use
                    // Mainly needed for ESC menu and default font due to no other font being supported
                    font.providers().none { it is SpaceFontProvider } ->
                        resourcePack.font(font.toBuilder().addProvider(fontSpaceProvider).build())
                    // If the font has already added an entry for the emote, skip it
                    font.providers().any { it is BitMapFontProvider && it.file() == emote.texture } ->
                        return@forEach emojy.logger.w("Skipping ${emote.id}-font because it is a bitmap and already added")
                }
            }

            val texture = textureFiles[emote.texture.value().substringAfterLast("/").removeSuffix(".png").plus(".png")]
            val vanillaTexture = ResourcePacks.vanillaResourcePack?.texture(emote.texture)

            if (texture == null && vanillaTexture == null)
                return@forEach emojy.logger.w("Could not find texture for ${emote.id}")

            texture?.also { Texture.texture(emote.texture, Writable.file(it)).addTo(resourcePack) }
            emote.appendFont(resourcePack)
        }

        emojy.gifs.forEach {
            it.generateSplitGif(resourcePack)
            it.font().addTo(resourcePack)
        }
        Font.font(emojyConfig.spaceFont, spaceProvider).addTo(resourcePack)

        if (emojyConfig.generateShader)
            generateGifShaderFiles(resourcePack)

        emojy.emotes.filter { it.atlas != null }.groupBy { it.atlas }.forEach { (atlas, emotes) ->
            val sources = emotes.map { AtlasSource.single(it.texture.removeSuffix(".png")) }
            val atlas = resourcePack.atlas(atlas!!)?.toBuilder() ?: Atlas.atlas().key(atlas)
            sources.forEach(atlas::addSource)
            resourcePack.atlas(atlas.build())
        }

        resourcePack.packMeta(emojyConfig.defaultPackFormat, "")

        MinecraftResourcePackWriter.minecraft().writeToZipFile(emojy.plugin.dataFolder.resolve("pack.zip"), resourcePack)
    }

    private fun generateGifShaderFiles(resourcePack: ResourcePack) {
        resourcePack.overlaysMeta(OverlaysMeta.of(
            OverlayEntry.of(PackFormat.format(34, 32, 34), "emojy_1_21_1"),
            OverlayEntry.of(PackFormat.format(42, 42, 46), "emojy_1_21_3"),
            OverlayEntry.of(PackFormat.format(63, 63, 99), "emojy_1_21_6")
        ))
        val classloader = emojy.plugin.javaClass.classLoader

        var path = "assets/minecraft/shaders/core/rendertype_text"
        resourcePack.unknownFile("emojy_1_21_1/$path.json", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_1/$path.json")!!))
        resourcePack.unknownFile("emojy_1_21_1/$path.fsh", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_1/$path.fsh")!!))
        resourcePack.unknownFile("emojy_1_21_1/$path.vsh", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_1/$path.vsh")!!))

        resourcePack.unknownFile("emojy_1_21_3/$path.json", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_3/$path.json")!!))
        resourcePack.unknownFile("emojy_1_21_3/$path.fsh", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_3/$path.fsh")!!))
        resourcePack.unknownFile("emojy_1_21_3/$path.vsh", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_3/$path.vsh")!!))

        resourcePack.unknownFile("emojy_1_21_6/$path.json", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_6/$path.json")!!))
        resourcePack.unknownFile("emojy_1_21_6/$path.fsh", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_6/$path.fsh")!!))
        resourcePack.unknownFile("emojy_1_21_6/$path.vsh", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_6/$path.vsh")!!))

        path += "_see_through"
        resourcePack.unknownFile("emojy_1_21_1/$path.json", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_1/$path.json")!!))
        resourcePack.unknownFile("emojy_1_21_1/$path.fsh", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_1/$path.fsh")!!))
        resourcePack.unknownFile("emojy_1_21_1/$path.vsh", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_1/$path.vsh")!!))

        resourcePack.unknownFile("emojy_1_21_3/$path.json", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_3/$path.json")!!))
        resourcePack.unknownFile("emojy_1_21_3/$path.fsh", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_3/$path.fsh")!!))
        resourcePack.unknownFile("emojy_1_21_3/$path.vsh", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_3/$path.vsh")!!))

        resourcePack.unknownFile("emojy_1_21_6/$path.json", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_6/$path.json")!!))
        resourcePack.unknownFile("emojy_1_21_6/$path.fsh", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_6/$path.fsh")!!))
        resourcePack.unknownFile("emojy_1_21_6/$path.vsh", Writable.copyInputStream(emojy.plugin.getResource("emojy_1_21_6/$path.vsh")!!))

        path = "assets/minecraft/shaders/include/emojy_gif_utils.glsl"
        resourcePack.unknownFile("emojy_1_21_6/$path", Writable.resource(classloader, "emojy_1_21_6/$path"))
    }
}
