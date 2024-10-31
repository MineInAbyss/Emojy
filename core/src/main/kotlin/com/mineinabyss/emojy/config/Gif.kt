package com.mineinabyss.emojy.config

import com.aaaaahhhhhhh.bananapuncher714.gifconverter.GifConverter
import com.mineinabyss.emojy.EmojyGenerator.gifFolder
import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.emojyConfig
import com.mineinabyss.emojy.spaceComponent
import com.mineinabyss.idofront.messaging.broadcastVal
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
import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.properties.Delegates

@Serializable
data class Gif(
    val id: String,
    @EncodeDefault(NEVER) var frameCount: Int = 0,
    @EncodeDefault(NEVER) @SerialName("framePath") val _framePath: @Serializable(KeySerializer::class) Key = Key.key(
        "${emojyConfig.defaultNamespace}:${emojyConfig.defaultFolder}/$id"
    ),
    @EncodeDefault(NEVER) val ascent: Int = 8,
    @EncodeDefault(NEVER) val height: Int = 8,
    @EncodeDefault(NEVER) var offset: Int? = null,
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

    private fun unicode(index: Int): String = Character.toChars(PRIVATE_USE_FIRST + index).first().toString()

    private fun unicode(): String {
        return when (type) {
            GifType.SHADER -> Component.text((0 until frameCount).joinToString(unicode(frameCount)) { unicode(it) })
                .font(font).color(TextColor.fromHexString("#FEFEFE")).serialize()

            GifType.OBFUSCATION -> Component.text(unicode(0), NamedTextColor.WHITE)
                .decorate(TextDecoration.OBFUSCATED).font(font).serialize()
        }
    }


    private fun calculateFramecount(): Int {
        if (frameCount <= 0) frameCount = runCatching {
            val reader = ImageIO.getImageReadersByFormatName("gif").next()
            reader.input = ImageIO.createImageInputStream(gifFile)
            aspectRatio = reader.getAspectRatio(0)
            if (aspectRatio % 1 != 0f && offset == null) {
                emojy.logger.w("AspectRatio for $id is not 1:1, and the offset (${-(height * aspectRatio).roundToInt() - 1}) between frames will likely be wrong")
                emojy.logger.w("Either modify your GIF's resolution to have an aspect-ration of 1:1 or tweak the offset-property of your glyph")
            }
            reader.getNumImages(true)
        }.onFailure {
            emojy.logger.d("Could not get frame count for ${id}.gif")
        }.getOrNull()?.apply { aspectRatio = 1f } ?: 0

        return frameCount
    }

    fun font() = Font.font(font, fontProvider(), gifAdvance())
    private fun gifAdvance() =
        FontProvider.space().advance(unicode(frameCount), offset ?:(-(height * aspectRatio).roundToInt() - 1))
            .build()

    private fun fontProvider() =
        FontProvider.bitMap(Key.key("${framePath.asString()}.png"), height, ascent, (0 until frameCount).map(::unicode))

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

            GifConverter.splitGif(gifFile, calculateFramecount())
            createSpritesheet(gifFolder)

            Texture.texture(Key.key("${framePath.asString()}.png"), Writable.file(gifSpriteSheet)).addTo(resourcePack)
            gifFolder.deleteRecursively()
        }.onFailure {
            emojy.logger.d("Could not generate split gif for ${id}.gif: ${it.message}")
        }
    }

    private fun createSpritesheet(gifFolder: File) {
        val frames = gifFolder.listFiles()
            ?.filter { it.isFile && it.extension == "png" }
            ?.sortedBy { it.nameWithoutExtension.toIntOrNull() }
            ?: throw IllegalStateException("No frame files found in $gifFolder")

        val (frameWidth, frameHeight) = ImageIO.read(frames.first()).let { it.width to it.height }
        val spritesheet = BufferedImage(frameWidth, frameCount * frameHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = spritesheet.createGraphics()
        graphics.composite = AlphaComposite.Src

        frames.forEachIndexed { index, frameFile ->
            val y = index * frameHeight
            graphics.drawImage(ImageIO.read(frameFile), 0, y, null)
        }
        graphics.dispose()

        ImageIO.write(spritesheet, "png", gifSpriteSheet)

    }
}