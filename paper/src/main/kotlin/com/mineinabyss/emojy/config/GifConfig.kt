@file:UseSerializers(KeySerializer::class)

package com.mineinabyss.emojy.config

import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.helpers.GifConverter
import com.mineinabyss.idofront.serialization.KeySerializer
import com.mineinabyss.idofront.util.appendSuffix
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.font.SpaceFontProvider
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

@Serializable
data class GifConfig(
    val id: String,
    @EncodeDefault(NEVER) val frameCount: Int = 0,
    @EncodeDefault(NEVER) val framePath: Key? = null,
    @EncodeDefault(NEVER) val ascent: Int? = null,
    @EncodeDefault(NEVER) val height: Int? = null,
    @EncodeDefault(NEVER) val offset: Int? = null,
    @EncodeDefault(NEVER) val type: Gif.GifType? = null,
) {
    fun resolve(config: EmojyConfig) = Gif(
        id = id,
        frameCount = frameCount,
        framePath = (framePath ?: Key.key("${config.defaultNamespace}:${config.defaultFolder}/${id}"))
            .asString().removeSuffix("/").let(Key::key),
        ascent = ascent ?: 8,
        height = height ?: 8,
        offset = offset,
        type = type ?: config.defaultGifType,
    )
}

val Gif.gifFile: File get() = emojy.dataFolder.resolve("gifs/${id}.gif").apply { parentFile.mkdirs() }

private fun Gif.calculateFramecount(): Int {
    val (fc, ar) = runCatching {
        val reader = ImageIO.getImageReadersByFormatName("gif").next()
        reader.input = ImageIO.createImageInputStream(gifFile)
        reader.getNumImages(true) to reader.getAspectRatio(0)
    }.onFailure {
        emojy.logger.d("Could not get frame count for ${id}.gif")
    }.getOrDefault(0 to 0f)

    if (frameCount <= 0) frameCount = fc
    if (aspectRatio <= 0) aspectRatio = ar

    return frameCount
}

fun Gif.toFont(): Font = Font.font(font, fontProvider(), gifAdvance())

private fun Gif.gifAdvance(): SpaceFontProvider {
    val offset = offset ?: (-(height * aspectRatio).roundToInt() - 1)
    return FontProvider.space().advance(unicode(frameCount), offset).build()
}

private fun Gif.fontProvider(): FontProvider {
    val characters = (0 until frameCount).map(::unicode)
    return FontProvider.bitMap(framePath.appendSuffix(".png"), height, ascent, characters)
}

fun Gif.generateSplitGif(resourcePack: ResourcePack) {
    runCatching {
        val gifFolder = emojy.generator.gifFolder.resolve(id)

        GifConverter(this, resourcePack).splitGif(calculateFramecount())
        gifFolder.deleteRecursively()
    }.onFailure {
        emojy.logger.d("Could not generate split gif for ${id}.gif: ${it.message}")
    }
}
