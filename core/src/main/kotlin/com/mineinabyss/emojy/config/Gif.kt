package com.mineinabyss.emojy.config

import com.aaaaahhhhhhh.bananapuncher714.gifconverter.GifConverter
import com.mineinabyss.emojy.EmojyGenerator.gifFolder
import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.emojyConfig
import com.mineinabyss.emojy.spaceComponent
import com.mineinabyss.idofront.serialization.KeySerializer
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.event.HoverEvent.hoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.texture.Texture
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.properties.Delegates

@Serializable
data class Gif(
    val id: String,
    @Transient var bitmapRows: Int = 0,
    @Transient var bitmapColumns: Int = 0,
    @EncodeDefault(NEVER) var frameCount: Int = 0,
    @EncodeDefault(NEVER) @SerialName("framePath") val _framePath: @Serializable(KeySerializer::class) Key = Key.key(
        "${emojyConfig.defaultNamespace}:${emojyConfig.defaultFolder}/$id"
    ),
    @EncodeDefault(NEVER) val ascent: Int = 8,
    @EncodeDefault(NEVER) val height: Int = 8,
    @EncodeDefault(NEVER) val type: GifType = emojyConfig.defaultGifType
) {
    @Transient val framePath = Key.key(_framePath.asString().removeSuffix("/"))
    @Transient val font = Key.key(framePath.namespace(), id)
    @Transient val permission = "emojy.gif.$id"
    @Transient val baseRegex = "(?<!\\\\):$id:".toRegex()
    @Transient val escapedRegex = "\\\\:$id:".toRegex()

    val gifFile by lazy { emojy.plugin.dataFolder.resolve("gifs/${id}.gif").apply { parentFile.mkdirs() } }
    val gifSpriteSheet by lazy { emojy.plugin.dataFolder.resolve("gifs/${id}.png").apply { parentFile.mkdirs() } }
    private var aspectRatio by Delegates.notNull<Float>()

    enum class GifType {
        SHADER, OBFUSCATION
    }

    private fun unicode(index: Int): Char = Character.toChars(PRIVATE_USE_FIRST + index).first()

    private fun unicode(): String {
        return when (type) {
            GifType.SHADER -> (0 until frameCount).joinToString("") { unicode(it).toString() }
                .miniMsg().font(font).color(TextColor.fromHexString("#FEFEFE")).serialize()

            GifType.OBFUSCATION -> unicode(0).toString().miniMsg()
                .decorate(TextDecoration.OBFUSCATED).font(font).color(NamedTextColor.WHITE).serialize()
        }
    }


    private fun frameCount(): Int {
        if (frameCount <= 0) frameCount = runCatching {
            val reader = ImageIO.getImageReadersByFormatName("gif").next()
            reader.input = ImageIO.createImageInputStream(gifFile)
            aspectRatio = reader.getAspectRatio(0)
            reader.getNumImages(true).also { frameCount ->
                bitmapColumns = ceil(sqrt(frameCount.toDouble())).toInt()
                bitmapRows = ceil(frameCount / bitmapColumns.toDouble()).toInt()
            }
        }.onFailure {
            emojy.logger.d("Could not get frame count for ${id}.gif")
        }.getOrNull()?.apply { aspectRatio = 1f } ?: 0

        return frameCount
    }

    fun font() = Font.font(font, fontProvider(), gifAdvance())
    private fun gifAdvance() =
        FontProvider.space().advance(unicode(frameCount() + 1).toString(), -(height * aspectRatio + 1).roundToInt())
            .build()

    private fun fontProvider(): FontProvider {
        // Construct the `chars` mapping in `["xyz", "xyz", "xyz"]` format
        val charsGrid = (0 until bitmapRows).map { row ->
            (0 until bitmapColumns).joinToString("") { col ->
                unicode(row + col * bitmapColumns).toString()
            }
        }

        return FontProvider.bitMap(Key.key("${framePath.asString()}.png"), height, ascent, charsGrid)
    }

    fun checkPermission(player: Player?) =
        !emojyConfig.requirePermissions || player == null || player.hasPermission(permission)

    fun formattedUnicode(appendSpace: Boolean = false, insert: Boolean = true): Component {
        var bitmap = unicode().miniMsg().font(font).color(TextColor.fromHexString("#FEFEFE"))

        bitmap = if (!insert) bitmap else bitmap.insertion(":${id}:").hoverEvent(
            hoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                ("<red>Type <i>:</i>$id<i>:</i> or <i><u>Shift + Click</i> this to use this emote").miniMsg()
            )
        )
        return if (appendSpace) Component.textOfChildren(bitmap, spaceComponent(2)) else bitmap
    }

    fun generateSplitGif(resourcePack: ResourcePack) {
        runCatching {
            val gifFolder = gifFolder.resolve(id)
            gifFolder.deleteRecursively()
            GifConverter.splitGif(gifFile, frameCount()) // Keep individual frame creation
            createSpritesheet(gifFolder).let {
                ImageIO.write(it, "png", gifSpriteSheet)
            } // Create the spritesheet based on frames

            Texture.texture(Key.key("${framePath.asString()}.png"), Writable.file(gifSpriteSheet)).addTo(resourcePack)
        }.onFailure {
            emojy.logger.d("Could not generate split gif for ${id}.gif: ${it.message}")
        }
    }

    private fun createSpritesheet(gifFolder: File): BufferedImage {
        // List all frame files in the folder, ensuring they are sorted by filename
        val frames = gifFolder.listFiles()
            ?.filter { it.isFile && it.extension == "png" }
            ?.sortedBy { it.nameWithoutExtension.toIntOrNull() } // Assumes frames are named in order (e.g., 1.png, 2.png)
            ?: throw IllegalStateException("No frame files found in $gifFolder")

        val frameCount = frames.size

        // Determine dimensions based on the first frame
        val firstFrame = ImageIO.read(frames[0])
        val (frameWidth, frameHeight) = firstFrame.width to firstFrame.height

        // Create the spritesheet image
        val spritesheet = BufferedImage(bitmapColumns * frameWidth, bitmapRows * frameHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = spritesheet.createGraphics()

        frames.forEachIndexed { index, frameFile ->
            val x = (index % bitmapColumns) * frameWidth
            val y = (index / bitmapColumns) * frameHeight
            val frameImage = ImageIO.read(frameFile)
            graphics.drawImage(frameImage, x, y, null)
        }
        graphics.dispose()

        return spritesheet
    }
}