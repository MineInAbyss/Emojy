package com.mineinabyss.emojy.config

import com.mineinabyss.emojy.EmojyGenerator.gifFolder
import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.emojyConfig
import com.mineinabyss.emojy.helpers.GifConverter
import com.mineinabyss.emojy.spaceComponent
import com.mineinabyss.idofront.serialization.KeySerializer
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import com.mineinabyss.idofront.util.appendSuffix
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
import net.kyori.adventure.text.`object`.ObjectContents
import org.bukkit.entity.Player
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.atlas.Atlas
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.font.SpaceFontProvider
import javax.imageio.ImageIO
import kotlin.math.roundToInt

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
    @Transient private var aspectRatio = 0f

    val gifFile by lazy { emojy.plugin.dataFolder.resolve("gifs/${id}.gif").apply { parentFile.mkdirs() } }
    val gifSpriteSheet by lazy { emojy.plugin.dataFolder.resolve("gifs/${id}.png").apply { parentFile.mkdirs() } }

    enum class GifType {
        SHADER, OBFUSCATION, SPRITE
    }

    private fun unicode(index: Int): String = Character.toChars(PRIVATE_USE_FIRST + index).first().toString()

    private fun unicode(): String {
        return when (type) {
            GifType.SHADER -> Component.text((0 until frameCount).joinToString(unicode(frameCount)) { unicode(it) })
                .font(font).color(TextColor.fromHexString("#FEFEFE")).serialize()

            GifType.OBFUSCATION -> Component.text(unicode(0), NamedTextColor.WHITE)
                .decorate(TextDecoration.OBFUSCATED).font(font).serialize()

            GifType.SPRITE -> Component.`object`(ObjectContents.sprite(Atlas.GUI, framePath)).serialize()
        }
    }


    private fun calculateFramecount(): Int {
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

    fun font() = Font.font(font, fontProvider(), gifAdvance())
    private fun gifAdvance(): SpaceFontProvider {
        val offset = offset ?: (-(height * aspectRatio).roundToInt() - 1)
        return FontProvider.space().advance(unicode(frameCount), offset).build()
    }

    private fun fontProvider(): FontProvider {
        val characters = (0 until frameCount).map(::unicode)
        return FontProvider.bitMap(framePath.appendSuffix(".png"), height, ascent, characters)
    }

    fun checkPermission(player: Player?): Boolean {
        return !emojyConfig.requirePermissions || player == null || player.hasPermission(permission)
    }

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

            GifConverter(this, resourcePack).splitGif(calculateFramecount())
            gifFolder.deleteRecursively()
        }.onFailure {
            emojy.logger.d("Could not generate split gif for ${id}.gif: ${it.message}")
        }
    }
}