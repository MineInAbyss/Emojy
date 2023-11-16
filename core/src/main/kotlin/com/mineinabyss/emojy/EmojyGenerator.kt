package com.mineinabyss.emojy

import com.aaaaahhhhhhh.bananapuncher714.gifconverter.GifConverter
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.emojy.config.Gifs
import com.mineinabyss.idofront.font.Space
import com.mineinabyss.idofront.font.Space.Companion.toNumber
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.messaging.logWarn
import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter
import team.unnamed.creative.texture.Texture
import java.io.File

object EmojyGenerator {
    private val gifFolder = emojy.plugin.dataFolder.resolve("gifs").apply { mkdirs() }
    private val emotesFolder = emojy.plugin.dataFolder.resolve("emotes").apply { mkdirs() }
    fun generateResourcePack() {
        val resourcePack = ResourcePack.resourcePack()
        File(emojy.plugin.dataFolder, "/assets").deleteRecursively()

        emojy.emotes.forEach { emote ->
            if (emote.isBitmap && resourcePack.font(emote.font) != null) return@forEach if (emojyConfig.debug) logWarn("Skipping ${emote.id}-font because it is a bitmap and already added") else {}
            resourcePack.font(emote.font())
            emotesFolder.listFiles()?.find { f -> f.nameWithoutExtension == emote.texture.value().substringAfterLast("/").removeSuffix(".png") }?.let {
                resourcePack.texture(Texture.texture(emote.texture, Writable.file(it)))
            } ?: if (emojyConfig.debug) logWarn("Could not find texture for ${emote.id}") else {}
        }
        emojy.gifs.forEach {
            it.generateSplitGif(resourcePack)
            resourcePack.font(it.font())
        }
        resourcePack.font(Font.font(emojyConfig.spaceFont, FontProvider.space(Space.entries.associate { it.unicode to it.toNumber() })))

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
            if (emojyConfig.debug) logError("Could not generate split gif for ${id}.gif: ${it.message}")
        }
    }
}
