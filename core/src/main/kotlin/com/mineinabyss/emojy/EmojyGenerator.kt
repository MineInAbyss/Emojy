package com.mineinabyss.emojy

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.messaging.logWarn
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO


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

            runCatching {
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
            }
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

    }

    val gifFolder = File(emojy.plugin.dataFolder,"gifs").run { mkdirs(); this }
    private fun EmojyConfig.Gif.generateSplitGif() {
        runCatching {
            val stream = FileInputStream(gifFolder.resolve("${id}.gif"))
            val decoder = GifDecoder()
            val frameCount = getFrameCount()
            decoder.read(stream)
            stream.close()

            var time = 0
            val totalTime = (0 until frameCount).sumOf { decoder.getDelay(it) }

            for (frameIndex in 0 until frameCount) {
                // Delay in 1/100th of a second
                val delay = decoder.getDelay(frameIndex)
                val start = time
                time += delay
                val end = time

                val frame = generateFrame(decoder.getFrame(frameIndex)!!, start, end, totalTime)
                val dest = gifFolder.resolve("${id}/${frameIndex + 1}.png").run { parentFile.mkdirs(); this }
                val assetDest = File(emojy.plugin.dataFolder.path, "/assets/${namespace}/textures/${imagePath}/${frameIndex + 1}.png").run { parentFile.mkdirs(); this }
                ImageIO.write(frame, "png", dest)
                ImageIO.write(frame, "png", assetDest)
            }
        }.onFailure(::logError)
    }

    private fun generateFrame(image: BufferedImage, start: Int, stop: Int, total: Int): BufferedImage {
        val frame = BufferedImage(image.width + 2, image.height + 2, BufferedImage.TYPE_INT_ARGB)
        frame.setRGB(
            1,
            1,
            image.width,
            image.height,
            image.getRGB(0, 0, image.width, image.height, null, 0, image.width),
            0,
            image.width
        )
        val width = frame.width - 1
        val height = frame.height - 1
        val info = IntArray(8)
        info[0] = total shr 8 and 0xFF
        info[1] = total and 0xFF
        info[2] = start shr 8 and 0xFF
        info[3] = start and 0xFF
        info[4] = stop shr 8 and 0xFF
        info[5] = stop and 0xFF

        // 0, 1, 2, 3 for quadrants
        // 149 for identifier
        // Other 2 are for relative coords
        frame.setRGB(width, 0, compact(0, 0, 0, 149))
        frame.setRGB(0, 0, compact(1, 0, 0, 149))
        frame.setRGB(0, height, compact(2, 0, 0, 149))
        frame.setRGB(width, height, compact(3, 0, 0, 149))
        frame.setRGB(width - 1, 0, compact(info[0], info[1], info[2], info[3]))
        frame.setRGB(width, 1, compact(info[4], info[5], info[6], info[7]))
        frame.setRGB(1, 0, compact(info[0], info[1], info[2], info[3]))
        frame.setRGB(0, 1, compact(info[4], info[5], info[6], info[7]))
        frame.setRGB(1, height, compact(info[0], info[1], info[2], info[3]))
        frame.setRGB(0, height - 1, compact(info[4], info[5], info[6], info[7]))
        frame.setRGB(width - 1, height, compact(info[0], info[1], info[2], info[3]))
        frame.setRGB(width, height - 1, compact(info[4], info[5], info[6], info[7]))
        return frame
    }

    private fun compact(b1: Int, b2: Int, b3: Int, b4: Int) = b4 and 0xFF shl 24 or (b1 and 0xFF shl 16) or (b2 and 0xFF shl 8) or (b3 and 0xFF)
}
