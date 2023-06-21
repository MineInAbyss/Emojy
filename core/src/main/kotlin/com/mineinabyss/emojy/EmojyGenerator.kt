package com.mineinabyss.emojy

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.messaging.logWarn
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO


//TODO Make font generation sort by namespace to avoid duplicate fonts
object EmojyGenerator {
    fun generateResourcePack() {
        File(emojy.dataFolder, "/assets").deleteRecursively()
        emojyConfig.emotes.forEach { emote ->
            val assetDir = File(emojy.dataFolder.path, "/assets").run { mkdirs(); this }
            try {
                val font = File(emojy.dataFolder, "/fonts/${emote.font.value()}.json")
                font.copyTo(assetDir.resolve(emote.namespace + "/font/${emote.font.value()}.json"), true)
            } catch (e: Exception) {
                if (emojyConfig.debug) when (e) {
                    is NoSuchFileException, is NullPointerException ->
                        logWarn("Could not find font ${emote.font.value()} for emote ${emote.id} in plugins/emojy/fonts")
                }
            }

            try {
                val texture =
                    File(emojy.dataFolder.path, "/textures/${emote.image}").run { parentFile.mkdirs(); this }
                texture.copyTo(assetDir.resolve(emote.namespace + "/textures/${emote.imagePath}"), true)
            } catch (e: Exception) {
                if (emojyConfig.debug) when (e) {
                    is NoSuchFileException, is NullPointerException -> {
                        logError("Could not find texture ${emote.image} for emote ${emote.id} in plugins/emojy/textures")
                        logWarn("Will not be copied to final resourcepack folder")
                        logWarn("If you have it through another resourcepack, ignore this")
                    }
                }
            }
        }

        emojyConfig.gifs.forEach { gif ->
            val assetDir = File(emojy.dataFolder.path, "/assets")
            try {
                val font = File(emojy.dataFolder, "/fonts/gifs/${gif.id}.json").run { parentFile.mkdirs(); this }
                font.copyTo(assetDir.resolve(gif.namespace + "/font/${gif.id}.json"), true)
            } catch (e: Exception) {
                if (emojyConfig.debug) when (e) {
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
        emojyConfig.emotes.forEach { emote ->
            fontFiles[emote.font.value()]?.add(emote.toJson())
                ?: fontFiles.putIfAbsent(emote.font.value(), JsonArray().apply { add(emote.toJson()) })
        }
        fontFiles.forEach { (font, array) ->
            val output = JsonObject()
            val fontFile = File("${emojy.dataFolder.absolutePath}/fonts/${font}.json")

            output.add("providers", array)
            fontFile.parentFile.mkdirs()
            fontFile.writeText(output.toString())
        }

        fontFiles.clear()

        emojyConfig.gifs.forEach { gif ->
            gif.toJson().forEach { json ->
                fontFiles[gif.id]?.add(json)
                    ?: fontFiles.putIfAbsent(gif.id, JsonArray().apply { add(json) })
            }
        }
        fontFiles.forEach { (font, array) ->
            val output = JsonObject()
            val fontFile = File("${emojy.dataFolder.absolutePath}/fonts/gifs/${font}.json")
            output.add("providers", array)
            fontFile.parentFile.mkdirs()
            fontFile.writeText(output.toString())
        }

    }

    fun reloadFontFiles() = generateFontFiles()

    val gifFolder = File(emojy.dataFolder,"gifs").run { mkdirs(); this }
    private fun EmojyConfig.Gif.generateSplitGif() {
        try {
            val imageReader = ImageIO.getImageReadersByFormatName("gif").next()
            imageReader.input = ImageIO.createImageInputStream(gifFolder.resolve("${id}.gif"))
            gifFolder.resolve(id).deleteRecursively() // Clear files for regenerating

            for (frameIndex in 0 until getFrameCount()) {
                val frame = imageReader.read(frameIndex)
                gifFolder.resolve("${id}/${frameIndex + 1}.png")
                val dest = gifFolder.resolve("${id}/${frameIndex + 1}.png").run { parentFile.mkdirs(); this }
                val assetDest = File(emojy.dataFolder.path, "/assets/${namespace}/textures/${imagePath}/${frameIndex + 1}.png").run { parentFile.mkdirs(); this }
                ImageIO.write(frame, "png", dest)
                ImageIO.write(frame, "png", assetDest)
            }

            imageReader.dispose()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
