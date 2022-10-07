package com.mineinabyss.emojy

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.emojy.EmojyGenerator.gifFolder
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.textcomponents.miniMsg
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.entity.Player
import javax.imageio.ImageIO
import javax.imageio.ImageReader

val emojyConfig get() = emojy.config.data
const val PRIVATE_USE_FIRST = 57344

@Serializable
data class EmojyConfig(
    val defaultNamespace: String = "emotes",
    val defaultFolder: String = "emotes",
    val defaultFont: String = "emotes",
    val defaultHeight: Int = 7,
    val defaultAscent: Int = 7,
    val requirePermissions: Boolean = true,
    val generateResourcePack: Boolean = true,
    val debug: Boolean = true,
    val emotes: MutableSet<Emote> = mutableSetOf(Emote()),
    val gifs: Set<Gif> = mutableSetOf(Gif())
) {
    @Serializable
    data class Emote(
        val id: String = "example",
        val font: String = emojyConfig.defaultFont,
        val texture: String = "${emojyConfig.defaultNamespace}:textures/${emojyConfig.defaultFolder}/$id.png",
        val height: Int = emojyConfig.defaultHeight,
        val ascent: Int = emojyConfig.defaultAscent,
    ) {
        // Beginning of Private Use Area \uE000 -> uF8FF
        // Option: (Character.toCodePoint('\uE000', '\uFF8F')/37 + getIndex())
        fun getUnicode(): Char =
            Character.toChars(PRIVATE_USE_FIRST + emojyConfig.emotes.filter { it.font == font }.map { it }
                .indexOf(this)).first()

        fun getFont() = Key.key(getNamespace(), font)
        fun getNamespace() = texture.substringBefore(":")
        fun getImage() = texture.substringAfterLast("/")
        fun getImagePath() = texture.substringAfter(":")
        fun getPermission() = "emojy.emote.$id"
        fun toJson(): JsonObject {
            val output = JsonObject()
            val chars = JsonArray()
            output.addProperty("type", "bitmap")
            output.addProperty("file", texture)
            output.addProperty("ascent", ascent)
            output.addProperty("height", height)
            chars.add(getUnicode())
            output.add("chars", chars)
            return output
        }

        fun checkPermission(player: Player?) =
            !emojyConfig.requirePermissions || player == null || player.hasPermission(getPermission()) || player.hasPermission(
                "emojy.font.${font}"
            )

        // TODO Change this to miniMsg(TagResolver) when Idofront is updated
        fun getFormattedUnicode(splitter: String = "", insert: Boolean = true): Component {
            val component = getUnicode().toString().miniMsg().mergeStyle(
                "".miniMsg().font(getFont()).color(NamedTextColor.WHITE).apply {
                    if (insert)
                        insertion(":${id}:")
                            .hoverEvent(
                                HoverEvent.hoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    ("<red>Type <i>:$id:</i> or <i>Shift + Click</i> this to use this emote").miniMsg()
                                )
                            )
                }
            )
            return if (emojyConfig.emotes.indexOf(this) == emojyConfig.emotes.size - 1) component
            else component.append("<font:default><white>$splitter</white></font>".miniMsg())
        }

        private val emojyTagResolver: TagResolver
            get() {
                val tagResolver = TagResolver.builder()
                emojyConfig.emotes.forEach { emote ->
                    Placeholder.component("emojy_$id", emote.getFormattedUnicode())
                }
                return tagResolver.build()
            }
    }


    @Serializable
    data class Gif(
        val id: String = "example",
        val frameCount: Int = 0,
        val framePath: String = "${emojyConfig.defaultNamespace}:textures/${emojyConfig.defaultFolder}/$id/",
        val ascent: Int = 8,
        val height: Int = 8,

        ) {
        fun getFont() = Key.key(getNamespace(), id)
        fun getNamespace() = framePath.substringBefore(":")
        fun getImagePath() = framePath.substringAfter(":")
        fun getPermission() = "emojy.gif.$id"
        fun getUnicode(i: Int): Char = Character.toChars(PRIVATE_USE_FIRST + i).first()

        @JvmName("getFrameCount1")
        fun getFrameCount(): Int {
            val reader: ImageReader = ImageIO.getImageReadersByFormatName("gif").next() as ImageReader
            val imageInput = ImageIO.createImageInputStream(gifFolder.resolve("${id}.gif"))

            reader.setInput(imageInput, false)
            return try {
                if (frameCount <= 0) reader.getNumImages(true) else frameCount
            } catch (e: IllegalStateException) {
                logError("Could not get frame count for ${id}.gif")
                return 0
            }
        }

        fun toJson(): MutableList<JsonObject> {
            val jsonList = mutableListOf<JsonObject>()
            (1..getFrameCount()).forEach { i ->
                val output = JsonObject()
                val chars = JsonArray()
                output.addProperty("type", "bitmap")
                output.addProperty("file", "$framePath$i.png")
                output.addProperty("ascent", ascent)
                output.addProperty("height", height)
                chars.add(getUnicode(i))
                output.add("chars", chars)
                jsonList.add(output)
            }
            return jsonList
        }

        fun checkPermission(player: Player?) =
            !emojyConfig.requirePermissions || player == null || player.hasPermission(getPermission())

        fun getFormattedUnicode(splitter: String = ""): Component {
            val component = getUnicode(1).toString().miniMsg().mergeStyle(
                Component.empty().font(getFont()).color(NamedTextColor.WHITE).insertion(":${id}:")
                    .decorate(TextDecoration.OBFUSCATED).hoverEvent(
                        HoverEvent.hoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            ("<red>Type <i>:$id:</i> or <i>Shift + Click</i> this to use this emote").miniMsg()
                        )
                    )
            )
            return if (emojyConfig.gifs.indexOf(this) == emojyConfig.gifs.size - 1) component
            else component.append("<font:default><white>$splitter</white></font>".miniMsg())
        }
    }

}
