package com.mineinabyss.emojy

import com.aaaaahhhhhhh.bananapuncher714.gifconverter.GifConverter
import com.mineinabyss.emojy.config.Gif
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
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.sqrt

object EmojyGenerator {
    val gifFolder = emojy.plugin.dataFolder.resolve("gifs").apply { mkdirs() }
    private val emotesFolder = emojy.plugin.dataFolder.resolve("emotes").apply { mkdirs() }
    private val spaceProvider = FontProvider.space(Space.entries.asSequence().filterNot(Space.NULL::equals).associate { it.unicode to it.toNumber() })

    fun generateResourcePack() {
        val resourcePack = ResourcePack.resourcePack()
        emojy.plugin.dataFolder.resolve("assets").deleteRecursively()

        val textureFiles = emotesFolder.walkTopDown().filter { it.isFile }.associateBy { it.name }
        val fontSpaceProvider = FontProvider.space().advance("\uE101", -1).build()
        emojy.emotes.forEach { emote ->
            resourcePack.font(emote.font)?.takeIf { emote.isMultiBitmap }?.let { font ->
                when {
                    // Add a -1 advance to the font for ease of use
                    // Mainly needed for ESC menu and default font due to no other font being supported
                    font.providers().none { it is SpaceFontProvider } ->
                        resourcePack.font(font.toBuilder().addProvider(fontSpaceProvider).build())
                    // If the font has already added an entry for the emote, skip it
                    font.providers().any { it is BitMapFontProvider && it.file() == emote.texture } ->
                        return@forEach emojy.logger.w("Skipping ${emote.id}-font because it is a bitmap and already added")
                            .let { null }
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
        Font.font(emojyConfig.spaceFont, spaceProvider).addTo(resourcePack)

        MinecraftResourcePackWriter.minecraft().writeToZipFile(emojy.plugin.dataFolder.resolve("pack.zip"), resourcePack)
    }
}
