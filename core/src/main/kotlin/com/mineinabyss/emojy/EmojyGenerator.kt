package com.mineinabyss.emojy

import com.aaaaahhhhhhh.bananapuncher714.gifconverter.GifConverter
import com.mineinabyss.emojy.config.Gifs
import com.mineinabyss.idofront.font.Space
import com.mineinabyss.idofront.font.Space.Companion.toNumber
import com.mineinabyss.idofront.resourcepacks.ResourcePacks
import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.font.BitMapFontProvider
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.font.SpaceFontProvider
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter
import team.unnamed.creative.texture.Texture

object EmojyGenerator {
    private val gifFolder = emojy.plugin.dataFolder.resolve("gifs").apply { mkdirs() }
    private val emotesFolder = emojy.plugin.dataFolder.resolve("emotes").apply { mkdirs() }

    fun generateResourcePack() {
        val resourcePack = ResourcePack.resourcePack()
        emojy.plugin.dataFolder.resolve("assets").deleteRecursively()

        val textureFiles = emotesFolder.walkTopDown().filter { it.isFile }.associateBy { it.name }
        val fontSpaceProvider = FontProvider.space().advance("\uE101", -1).build()
        emojy.emotes.mapNotNull { emote ->
            resourcePack.font(emote.font)?.takeIf { emote.isMultiBitmap }?.let { font ->
                when {
                    // Add a -1 advance to the font for ease of use
                    // Mainly needed for ESC menu and default font due to no other font being supported
                    font.providers().none { it is SpaceFontProvider } ->
                        resourcePack.font(font.toBuilder().addProvider(fontSpaceProvider).build())
                    // If the font has already added an entry for the emote, skip it
                    font.providers().any { it is BitMapFontProvider && it.file() == emote.texture } ->
                        return@mapNotNull emojy.logger.w("Skipping ${emote.id}-font because it is a bitmap and already added").let { null }
                }
            }

            emote.appendFont(resourcePack).addTo(resourcePack)
            val texture = textureFiles[emote.texture.value().substringAfterLast("/")]
            if (texture == null && ResourcePacks.defaultVanillaResourcePack?.texture(emote.texture) == null)
                emojy.logger.w("Could not find texture for ${emote.id}")
            Texture.texture(emote.texture, texture?.let(Writable::file) ?: Writable.EMPTY).addTo(resourcePack)
        }

        emojy.gifs.forEach {
            it.generateSplitGif(resourcePack)
            it.font().addTo(resourcePack)
        }
        Font.font(emojyConfig.spaceFont, FontProvider.space(Space.entries.filterNot(Space.NULL::equals)
            .associate { it.unicode to it.toNumber() })).addTo(resourcePack)

        MinecraftResourcePackWriter.minecraft().writeToZipFile(emojy.plugin.dataFolder.resolve("pack.zip"), resourcePack)
    }

    private fun Gifs.Gif.generateSplitGif(resourcePack: ResourcePack) {
        runCatching {
            gifFolder.resolve(id).deleteRecursively()
            GifConverter.splitGif(gifFile, frameCount())
            gifFolder.resolve(id).listFiles()?.filterNotNull()?.map {
                Texture.texture(Key.key("${framePath.asString()}${it.name}"), Writable.file(it))
            }?.forEach(resourcePack::texture)
        }.onFailure {
            emojy.logger.d("Could not generate split gif for ${id}.gif: ${it.message}")
        }
    }
}
