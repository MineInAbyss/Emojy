package com.mineinabyss.emojy

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.messaging.logWarn
import org.w3c.dom.Node
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.ImageReader


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

        emojyConfig.gifs.forEach { gif ->
            val assetDir = File(emojy.dataFolder.path, "/assets")
            try {
                val font = File(emojy.dataFolder, "/fonts/gifs/${gif.id}.json").run { parentFile.mkdirs(); this }
                font.copyTo(assetDir.resolve(gif.getNamespace() + "/font/gifs/${gif.id}.json"), true)
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

        fontFiles.clear()

        emojyConfig.gifs.forEach { gif ->
            gif.toJson().forEach { json ->
                fontFiles[gif.getFont().toString()]?.add(json)
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
            val reader: ImageReader = ImageIO.getImageReadersByFormatName("gif").next() as ImageReader
            val imageInput = ImageIO.createImageInputStream(gifFolder.resolve("${id}.gif"))

            gifFolder.resolve(id).deleteRecursively() // Clear files for regenerating
            reader.setInput(imageInput, false)
            val noi: Int = getFrameCount()
            val imageatt = arrayOf("imageLeftPosition", "imageTopPosition", "imageWidth", "imageHeight")
            var master: BufferedImage? = null
            for (i in 0 until noi) {
                val tree: Node = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0")

                for (j in 0 until tree.childNodes.length) {
                    val nodeItem: Node = tree.childNodes.item(j)
                    if (nodeItem.nodeName.equals("ImageDescriptor")) {
                        val imageAttr: MutableMap<String, Int> = mutableMapOf()
                        imageatt.indices.forEach {
                            val attnode: Node = nodeItem.attributes.getNamedItem(imageatt[it])
                            imageAttr[imageatt[it]] = Integer.valueOf(attnode.nodeValue) ?: return
                        }
                        if (i == 0) {
                            master = BufferedImage(imageAttr["imageWidth"]!!, imageAttr["imageHeight"]!!, BufferedImage.TYPE_INT_ARGB)
                        }
                        master?.graphics?.drawImage(reader.read(i), imageAttr["imageLeftPosition"]!!, imageAttr["imageTopPosition"]!!, null)
                    }
                }
                val dest = gifFolder.resolve("${id}/${i + 1}.png").run { parentFile.mkdirs(); this }
                val assetDest = File(emojy.dataFolder.path, "/assets/${getNamespace()}/textures/${getImagePath()}/${i + 1}.png").run { parentFile.mkdirs(); this }
                ImageIO.write(master, "PNG", dest)
                ImageIO.write(master, "PNG", assetDest)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
