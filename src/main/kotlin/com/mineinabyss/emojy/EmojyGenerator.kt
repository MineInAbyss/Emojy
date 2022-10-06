package com.mineinabyss.emojy

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.messaging.logWarn
import java.io.File

//TODO Make font generation sort by namespace to avoid duplicate fonts
object EmojyGenerator {

    fun generateResourcePack() {
        File(emojy.dataFolder, "/assets").deleteRecursively()
        emojyConfig.emotes.forEach { emote ->
            val assetDir = File(emojy.dataFolder.path, "/assets").run { mkdirs(); this }
            try {
                val font = File(emojy.dataFolder, "/fonts/${emote.font}.json")
                font.copyTo(assetDir.resolve(emote.getNamespace() + "/font/${emote.font}.json"), true)
            } catch (e: Exception) {
                if (emojyConfig.debug) when (e) {
                    is NoSuchFileException, is NullPointerException ->
                        logWarn("Could not find font ${emote.font} for emote ${emote.id} in plugins/emojy/fonts")
                }
            }

            try {
                val texture =
                    File(emojy.dataFolder.path, "/textures/${emote.getImage()}").run { parentFile.mkdirs(); this }
                texture.copyTo(assetDir.resolve(emote.getNamespace() + "/textures/${emote.getImagePath()}"), true)
            } catch (e: Exception) {
                if (emojyConfig.debug) when (e) {
                    is NoSuchFileException, is NullPointerException -> {
                        logError("Could not find texture ${emote.getImage()} for emote ${emote.id} in plugins/emojy/textures")
                        logWarn("Will not be copied to final resourcepack folder")
                        logWarn("If you have it through another resourcepack, ignore this")
                    }
                }
            }
        }
    }

    fun generateFontFiles() {
        val fontFiles = mutableMapOf<String, JsonArray>()
        emojyConfig.emotes.forEach { emote ->
            fontFiles[emote.font]?.add(emote.toJson())
                ?: fontFiles.putIfAbsent(emote.font, JsonArray().apply { add(emote.toJson()) })
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
