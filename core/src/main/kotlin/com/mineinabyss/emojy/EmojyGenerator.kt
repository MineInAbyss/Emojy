package com.mineinabyss.emojy

import com.aaaaahhhhhhh.bananapuncher714.gifconverter.GifConverter
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.emojy.config.Gifs
import com.mineinabyss.idofront.font.Space
import com.mineinabyss.idofront.font.Space.Companion.toNumber
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.messaging.logWarn
import java.io.File


//TODO Make font generation sort by namespace to avoid duplicate fonts
object EmojyGenerator {
    fun generateResourcePack() {
        File(emojy.plugin.dataFolder, "/assets").deleteRecursively()
        emojy.emotes.forEach { emote ->
            val assetDir = File(emojy.plugin.dataFolder.path, "/assets").apply { mkdirs() }
            runCatching {
                val font = File(emojy.plugin.dataFolder, "/fonts/${emote.font.value()}.json")
                font.copyTo(assetDir.resolve("${emote.font.namespace()}/font/${emote.font.value()}.json"), true)
            }.getOrElse {
                if (emojyConfig.debug) when (it) {
                    is NoSuchFileException, is NullPointerException ->
                        logWarn("Could not find font ${emote.font.asString()} for emote ${emote.id} in plugins/emojy/fonts")
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

        emojy.gifs.forEach { gif ->
            val assetDir = emojy.plugin.dataFolder.resolve("assets")
            runCatching {
                val font = File(emojy.plugin.dataFolder, "/fonts/gifs/${gif.id}.json").run { parentFile.mkdirs(); this }
                font.copyTo(assetDir.resolve(gif.framePath.namespace() + "/font/${gif.id}.json"), true)
            }.getOrElse {
                if (emojyConfig.debug) when (it) {
                    is NoSuchFileException, is NullPointerException ->
                        logWarn("Could not find font ${gif.id} for emote ${gif.id} in plugins/emojy/fonts")
                }
            }

            gif.generateSplitGif()
        }

        // Copy space-font
        emojy.plugin.dataFolder.resolve("fonts/${emojyConfig.spaceFont.value()}.json").apply {
            parentFile.mkdirs()
            copyTo(emojy.plugin.dataFolder.resolve("assets/${emojyConfig.spaceFont.namespace()}/font/${emojyConfig.spaceFont.value()}.json"), true)
        }
    }

    fun generateFontFiles() {
        val fontFiles = mutableMapOf<String, JsonArray>()
        emojy.emotes.forEach { emote ->
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
            if (font == "default" && emojyConfig.supportForceUnicode)
                fontFolder.resolve("uniform.json").writeText(output.toString())
            fontFile.writeText(output.toString())
        }
        fontFiles.clear()

        emojy.gifs.forEach { gif ->
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
        emojy.plugin.dataFolder.resolve("fonts/${emojyConfig.spaceFont.value()}.json").apply {
            parentFile.mkdirs()
            writeText(JsonObject().apply {
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
            }.toString())
        }
    }

    val gifFolder = emojy.plugin.dataFolder.resolve("gifs").apply { mkdirs() }
    private fun Gifs.Gif.generateSplitGif() {
        runCatching {
            gifFolder.resolve(id).deleteRecursively() // Clear files for regenerating
            GifConverter.splitGif(gifFolder.resolve("${id}.gif"), frameCount())
            gifFolder.resolve(id).copyRecursively(emojy.plugin.dataFolder.resolve("assets/${framePath.namespace()}/textures/${framePath.value()}"), true)
        }.onFailure {
            if (emojyConfig.debug) logError("Could not generate split gif for ${id}.gif: ${it.message}")
        }
    }
}
