package com.mineinabyss.emojy

import com.aaaaahhhhhhh.bananapuncher714.gifconverter.GifConverter
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.idofront.font.Space
import com.mineinabyss.idofront.font.Space.Companion.toNumber
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.messaging.logWarn
import net.kyori.adventure.key.Key
import java.io.File


//TODO Make font generation sort by namespace to avoid duplicate fonts
object EmojyGenerator {
    fun generateResourcePack() {
        File(emojy.plugin.dataFolder, "/assets").deleteRecursively()
        emojy.config.emotes.forEach { emote ->
            val assetDir = File(emojy.plugin.dataFolder.path, "/assets").run { mkdirs(); this }
            runCatching {
                val font = File(emojy.plugin.dataFolder, "/fonts/${emote.font.value()}.json")
                font.copyTo(assetDir.resolve(emote.namespace + "/font/${emote.font.value()}.json"), true)
            }.getOrElse {
                if (emojy.config.debug) when (it) {
                    is NoSuchFileException, is NullPointerException ->
                        logWarn("Could not find font ${emote.font.value()} for emote ${emote.id} in plugins/emojy/fonts")
                }
            }

            //TODO Remove or find a better solution (files cant have same names here and its annoying either way)
            /*runCatching {
                val texture =
                    File(emojy.plugin.dataFolder.path, "/textures/${emote.image}").run { parentFile.mkdirs(); this }
                texture.copyTo(assetDir.resolve(emote.namespace + "/textures/${emote.imagePath}"), true)
            }.getOrElse {
                if (emojy.config.debug) when (it) {
                    is NoSuchFileException, is NullPointerException -> {
                        logError("Could not find texture ${emote.image} for emote ${emote.id} in plugins/emojy/textures")
                        logWarn("Will not be copied to final resourcepack folder")
                        logWarn("If you have it through another resourcepack, ignore this")
                    }
                }
            }*/
        }

        emojy.config.gifs.forEach { gif ->
            val assetDir = File(emojy.plugin.dataFolder.path, "/assets")
            runCatching {
                val font = File(emojy.plugin.dataFolder, "/fonts/gifs/${gif.id}.json").run { parentFile.mkdirs(); this }
                font.copyTo(assetDir.resolve(gif.namespace + "/font/${gif.id}.json"), true)
            }.getOrElse {
                if (emojy.config.debug) when (it) {
                    is NoSuchFileException, is NullPointerException ->
                        logWarn("Could not find font ${gif.id} for emote ${gif.id} in plugins/emojy/fonts")
                }
            }

            //TODO Copy all the split images into resourcepack
            gif.generateSplitGif()
        }
    }

    fun generateFontFiles() {
        val fontFiles = mutableMapOf<String, JsonArray>()
        emojy.config.emotes.forEach { emote ->
            fontFiles[emote.font.value()]?.add(emote.toJson())
                ?: fontFiles.putIfAbsent(emote.font.value(), JsonArray().apply { add(emote.toJson()) })
        }
        val fontFolder = File("${emojy.plugin.dataFolder.absolutePath}/fonts/")
        fontFolder.deleteRecursively()

        fontFiles.forEach { (font, array) ->
            val output = JsonObject()
            val fontFile = fontFolder.resolve("${font}.json")

            output.add("providers", array)
            fontFile.parentFile.mkdirs()
            if (font == "default" && emojy.config.supportForceUnicode)
                fontFolder.resolve("uniform.json").writeText(output.toString())
            fontFile.writeText(output.toString())
        }
        fontFiles.clear()

        emojy.config.gifs.forEach { gif ->
            gif.toJson().forEach { json ->
                fontFiles[gif.id]?.add(json)
                    ?: fontFiles.putIfAbsent(gif.id, JsonArray().apply { add(json) })
            }
        }

        fontFiles.forEach { (font, array) ->
            val output = JsonObject()
            val fontFile = File("${emojy.plugin.dataFolder.absolutePath}/fonts/gifs/${font}.json")
            output.add("providers", array)
            fontFile.parentFile.mkdirs()
            fontFile.writeText(output.toString())
        }
        fontFiles.clear()

        // Generate space-font
        val spaceKey = emojy.config.spaceFont.let { Key.key(it.substringBefore(":", "minecraft"), it.substringAfter(":", it)) }
        emojy.plugin.dataFolder.resolve("fonts/${spaceKey.key().value()}.json").apply {
            parentFile.mkdirs()
            createNewFile()
        }.writeText(
            JsonObject().apply {
                add("providers", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "space")
                        add("advances", JsonObject().apply {
                            Space.entries.filter { it.unicode.isNotEmpty() }.map { space -> space.toNumber() to space.unicode }.forEach { (number, unicode) ->
                                addProperty(unicode, number)
                            }
                        })
                    })
                })
            }.toString()
        )

    }

    val gifFolder = File(emojy.plugin.dataFolder,"gifs").run { mkdirs(); this }
    private fun EmojyConfig.Gif.generateSplitGif() {
        runCatching {
            gifFolder.resolve(id).deleteRecursively() // Clear files for regenerating
            GifConverter.splitGif( gifFolder.resolve("${id}.gif"), getFrameCount())
        }.onFailure {
            if (emojy.config.debug) logError("Could not generate split gif for ${id}.gif: ${it.message}")
        }
    }
}
