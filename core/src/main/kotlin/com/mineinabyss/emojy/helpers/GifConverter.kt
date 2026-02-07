package com.mineinabyss.emojy.helpers

import com.mineinabyss.emojy.config.Gif
import com.mineinabyss.idofront.util.appendSuffix
import com.mineinabyss.idofront.util.replace
import team.unnamed.creative.base.Writable
import team.unnamed.creative.texture.Texture
import java.awt.AlphaComposite
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object GifConverter {

    fun splitGif(gif: Gif, frameCount: Int): Texture {
        val decoder = GifDecoder()
        gif.gifFile.inputStream().use(decoder::read)
        decoder.setFrameCount(frameCount)

        var time = 0
        val totalTime = (0 until decoder.getFrameCount()).sumOf(decoder::getDelay)

        val frames = (0 until decoder.getFrameCount()).mapNotNull {
            val delay = decoder.getDelay(it)
            val start = time
            time += delay
            val end = time
            val image = toBufferedImage(decoder.getFrame(it) ?: return@mapNotNull null)
            generateFrame(image, start, end, totalTime)
        }

        return createSpritesheet(frames, gif)
    }

    private fun createSpritesheet(frames: List<BufferedImage>, gif: Gif): Texture {
        val (width, height) = frames.first().let { it.width to it.height }
        val spritesheet = BufferedImage(frames.size * width, height, BufferedImage.TYPE_INT_ARGB)
        val g = spritesheet.createGraphics()
        g.composite = AlphaComposite.Src

        frames.forEachIndexed { i, frame ->
            g.drawImage(frame, i * width, 0, width, height, null)
        }
        g.dispose()

        val data = runCatching {
            ByteArrayOutputStream().use { out ->
                ImageIO.write(spritesheet, "png", out)
                out.toByteArray()
            }
        }.onFailure { it.printStackTrace() }.getOrDefault(byteArrayOf())

        return Texture.texture(gif.framePath.appendSuffix(".png"), Writable.bytes(data))
    }

    private fun generateFrame(image: BufferedImage, start: Int, stop: Int, total: Int): BufferedImage {
        val (iWidth, iHeight) = image.width to image.height
        val frame = BufferedImage(iWidth + 2, iHeight + 2, BufferedImage.TYPE_INT_ARGB)
        frame.setRGB(1, 1, iWidth, iHeight, image.getRGB(0, 0, iWidth, iHeight, null, 0, iWidth), 0, iWidth)

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