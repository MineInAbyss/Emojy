package com.mineinabyss.emojy

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.idofront.messaging.logWarn
import java.io.File

//TODO Make font generation sort by namespace to avoid duplicate fonts
object EmojyGenerator {

    fun generateResourcePack() {
        emojyConfig.emotes.forEach { emote ->
            val font = File(emojy.dataFolder, "/fonts/${emote.font}.json")
            val texture = File(emojy.dataFolder.path, "/textures/${emote.getImage()}")
            val assetDir = File(emojy.dataFolder.path, "/assets")
            texture.parentFile.mkdirs()
            assetDir.mkdirs()

            try {
                font.copyTo(assetDir.resolve(emote.getNamespace() + "/font/${emote.font}.json"), true)
                texture.copyTo(assetDir.resolve(emote.getNamespace() + "/textures/${emote.getImage()}"), true)
            } catch (e: NoSuchFileException) {
                logWarn("Could not find ${e.file} for emote ${emote.id}")
            }
        }
    }

    fun generateFontFiles() {
        val fontFiles = mutableMapOf<String, JsonArray>()

        emojyConfig.emotes.forEach { emote ->
            val array = JsonArray()
            array.add(emote.toJson())
            fontFiles[emote.font]?.add(array) ?: fontFiles.putIfAbsent(emote.font, array)
        }

        fontFiles.forEach { (font, array) ->
            val output = JsonObject()
            val fontFile = File("${emojy.dataFolder.absolutePath}/fonts/${font}.json")

            output.add("providers", array)
            fontFile.parentFile.mkdirs()
            fontFile.writeText(output.toString())
        }
    }

    fun reloadFontFiles() = generateFontFiles()
}
