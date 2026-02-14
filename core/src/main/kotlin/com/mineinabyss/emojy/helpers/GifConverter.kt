package com.mineinabyss.emojy.helpers

import com.mineinabyss.emojy.config.Gif
import com.mineinabyss.emojy.emojy
import com.mineinabyss.idofront.util.appendSuffix
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.metadata.Metadata
import team.unnamed.creative.metadata.animation.AnimationMeta
import team.unnamed.creative.texture.Texture
import java.awt.AlphaComposite
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.min

class GifConverter(val gif: Gif, val resourcePack: ResourcePack) {

    fun splitGif(frameCount: Int) {
        val decoder = GifDecoder()
        gif.gifFile.inputStream().use(decoder::read)
        decoder.setFrameCount(frameCount)

        val frames = buildList {
            if (gif.type == Gif.GifType.SPRITE) repeat(decoder.getFrameCount()) { this += decoder.getFrame(it)?.let(::toBufferedImage) ?: return@repeat }
            else {
                var time = 0
                val totalTime = (0 until decoder.getFrameCount()).sumOf(decoder::getDelay)
                repeat(decoder.getFrameCount()) {
                    val delay = decoder.getDelay(it)
                    val start = time
                    time += delay
                    val end = time
                    val image = toBufferedImage(decoder.getFrame(it)?.let(::toBufferedImage) ?: return@repeat)
                    this += generateFrame(image, start, end, totalTime)
                }
            }
        }

        resourcePack.texture(createSpritesheet(frames))
    }

    private fun createSpritesheet(frames: List<BufferedImage>): Texture {
        val (width, height) = frames.first().let { it.width to it.height }
        val spritesheet = BufferedImage(width, frames.size * height, BufferedImage.TYPE_INT_ARGB)
        val g = spritesheet.createGraphics()
        g.composite = AlphaComposite.Src

        frames.forEachIndexed { i, frame ->
            g.drawImage(frame, 0, i * height, width, height, null)
        }
        g.dispose()

        val data = Writable.bytes(runCatching {
            ByteArrayOutputStream().use { out ->
                ImageIO.write(spritesheet, "png", out)
                out.toByteArray()
            }
        }.onFailure { it.printStackTrace() }.getOrDefault(byteArrayOf()))

        return if (gif.type == Gif.GifType.SPRITE) {
            val meta = AnimationMeta.animation().width(width).height(height).build()
            Texture.texture(gif.framePath.appendSuffix(".png"), data, Metadata.metadata().addPart(meta).build())
        } else Texture.texture(gif.framePath.appendSuffix(".png"), data)
    }

    private fun generateFrame(image: BufferedImage, start: Int, stop: Int, total: Int): BufferedImage {
        val (iWidth, iHeight) = min(image.width + 2, 256) to min(image.height + 2, 256)
        val frame = BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_ARGB)
        frame.setRGB(1, 1, iWidth-2, iHeight-2, image.getRGB(0, 0, iWidth-2, iHeight-2, null, 0, iWidth-2), 0, iWidth-2)

        val width = frame.width - 1
        val height = frame.height - 1
        val info = intArrayOf(total shr 8 and 255, total and 255, start shr 8 and 255, start and 255, stop shr 8 and 255, stop and 255, 0, 0)

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

    private fun compact(b1: Int, b2: Int, b3: Int, b4: Int): Int {
        return (b4 and 0xFF shl 24) or (b1 and 0xFF shl 16) or (b2 and 0xFF shl 8) or (b3 and 0xFF)
    }

    fun toBufferedImage(img: Image): BufferedImage {
        if (img is BufferedImage && img.type == BufferedImage.TYPE_INT_ARGB) return img
        val bimage = BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB)
        val bGr = bimage.createGraphics()
        bGr.drawImage(img, 0, 0, null)
        bGr.dispose()
        return bimage
    }
}