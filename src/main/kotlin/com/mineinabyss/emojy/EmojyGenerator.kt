package com.mineinabyss.emojy

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.messaging.logWarn
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.metadata.IIOMetadata


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
                val font = File(emojy.dataFolder, "/fonts/gifs/${gif.id}.json")
                font.copyTo(assetDir.resolve(gif.getNamespace() + "/font/${gif.id}.json"), true)
            } catch (e: Exception) {
                if (emojyConfig.debug) when (e) {
                    is NoSuchFileException, is NullPointerException ->
                        logWarn("Could not find font ${gif.id} for emote ${gif.id} in plugins/emojy/fonts")
                }
            }

            try {
                val imageatt = arrayOf(
                    "imageLeftPosition",
                    "imageTopPosition",
                    "imageWidth",
                    "imageHeight"
                )
                val gifFolder = File(emojy.dataFolder,"gifs/").run { mkdirs(); this }
                val reader: ImageReader = ImageIO.getImageReadersByFormatName("gif").next() as ImageReader
                val ciis = ImageIO.createImageInputStream(gifFolder.resolve("${gif.id}.gif"))
                reader.setInput(ciis, false)
                val noi: Int = if (gif.frameCount <= 0) reader.getNumImages(true) else gif.frameCount
                var master: BufferedImage? = null
                for (i in 0 until noi) {
                    val image: BufferedImage = reader.read(i)
                    val metadata: IIOMetadata = reader.getImageMetadata(i)
                    val tree: Node = metadata.getAsTree("javax_imageio_gif_image_1.0")
                    val children: NodeList = tree.childNodes
                    for (j in 0 until children.length) {
                        val nodeItem: Node = children.item(j)
                        if (nodeItem.nodeName.equals("ImageDescriptor")) {
                            val imageAttr: MutableMap<String, Int> = HashMap()
                            for (k in imageatt.indices) {
                                val attr: NamedNodeMap = nodeItem.attributes
                                val attnode: Node = attr.getNamedItem(imageatt[k])
                                imageAttr[imageatt[k]] = Integer.valueOf(attnode.nodeValue)
                            }
                            if (i == 0) {
                                master = BufferedImage(
                                    imageAttr["imageWidth"]!!,
                                    imageAttr["imageHeight"]!!, BufferedImage.TYPE_INT_ARGB
                                )
                            }
                            master!!.graphics.drawImage(
                                image,
                                imageAttr["imageLeftPosition"]!!, imageAttr["imageTopPosition"]!!, null
                            )
                        }
                    }
                    ImageIO.write(master, "GIF", File("$i.gif"))
                }
            } catch (e: IOException) {
                e.printStackTrace()
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
