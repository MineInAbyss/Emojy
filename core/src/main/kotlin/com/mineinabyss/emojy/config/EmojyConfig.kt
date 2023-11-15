@file:OptIn(ExperimentalSerializationApi::class)

package com.mineinabyss.emojy.config

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.emojy.EmojyGenerator.gifFolder
import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.emojyConfig
import com.mineinabyss.emojy.space
import com.mineinabyss.emojy.templates
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.serialization.KeySerializer
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import kotlinx.serialization.*
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.Transient
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.event.HoverEvent.hoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.properties.Delegates

const val PRIVATE_USE_FIRST = 57344
const val SPACE_PERMISSION = "emojy.space"

@Serializable
data class EmojyConfig(
    val defaultNamespace: String = "emotes",
    val defaultFolder: String = "emotes",
    val defaultFont: @Serializable(KeySerializer::class) Key = Key.key("emotes:emotes"),
    val defaultHeight: Int = 8,
    val defaultAscent: Int = 8,
    val spaceFont: @Serializable(KeySerializer::class) Key = Key.key("minecraft:space"),

    val requirePermissions: Boolean = true,
    val generateResourcePack: Boolean = true,
    val supportForceUnicode: Boolean = true,
    val debug: Boolean = true,
    val emojyList: EmojyList = EmojyList(),
    val supportedLanguages: Set<String> = mutableSetOf("en_us"),

    ) {

    enum class ListType { BOOK, BOOK2, CHAT }

    @Serializable
    data class EmojyList(
        val type: ListType = ListType.CHAT,
        val ignoredEmoteIds: Set<String> = mutableSetOf(),
        val ignoredGifIds: Set<String> = mutableSetOf(),
        val ignoredFonts: Set<@Serializable(KeySerializer::class) Key> = mutableSetOf()
    ) {
        val ignoredEmotes: Set<Emotes.Emote>
            get() = emojy.emotes.filter { it.id in ignoredEmoteIds || it.font in ignoredFonts }.toSet()
        val ignoredGifs: Set<Gifs.Gif>
            get() = emojy.gifs.filter { it.id in ignoredGifIds || it.font in ignoredFonts }.toSet()
    }
}

@Serializable
data class Emotes(val emotes: Set<Emote> = mutableSetOf()) {

    @Serializable
    data class Emote(
        val id: String,
        @SerialName("template") @EncodeDefault(NEVER) private val _template: String? = null,
        @EncodeDefault(NEVER) @Transient val template: EmojyTemplate? = templates.find { it.id == _template },

        @EncodeDefault(NEVER) val font: @Serializable(KeySerializer::class) Key =
            template?.font ?: emojyConfig.defaultFont,
        @EncodeDefault(NEVER) val texture: @Serializable(KeySerializer::class) Key =
            template?.texture ?: Key.key("${emojyConfig.defaultNamespace}:${emojyConfig.defaultFolder}/${id.lowercase()}.png"),
        @EncodeDefault(NEVER) val height: Int = template?.height ?: emojyConfig.defaultHeight,
        @EncodeDefault(NEVER) val ascent: Int = template?.ascent ?: emojyConfig.defaultAscent,
        @EncodeDefault(NEVER) val bitmapWidth: Int = template?.bitmapWidth ?: 1,
        @EncodeDefault(NEVER) val bitmapHeight: Int = template?.bitmapHeight ?: 1,
    ) {
        // Beginning of Private Use Area \uE000 -> uF8FF
        // Option: (Character.toCodePoint('\uE000', '\uFF8F')/37 + getIndex())
        @EncodeDefault(NEVER) @Transient private val lastUsedUnicode: MutableMap<Key, Int> = mutableMapOf()
        private fun unicodes(): MutableList<String> {
            return mutableListOf("").apply {
                for (i in 0 until bitmapHeight) {
                    for (j in 0 until bitmapWidth) {
                        val lastUnicode = lastUsedUnicode[font] ?: 0
                        val row = ((getOrNull(i) ?: "") + Character.toChars(
                            PRIVATE_USE_FIRST + lastUnicode + emojy.emotes
                                .filter { it.font == font }.map { it }.indexOf(this@Emote)
                        ).firstOrNull().toString())
                        if (getOrNull(i) == null)
                            add(i, row) else set(i, row)
                        lastUsedUnicode.put(font, lastUnicode + 1) ?: lastUsedUnicode.putIfAbsent(font, 1)
                    }
                }
                lastUsedUnicode.clear()
            }
        }

        private val permission get() = "emojy.emote.$id"
        private val fontPermission get() = "emojy.font.$font"
        fun toJson(): JsonObject {
            val output = JsonObject()
            output.addProperty("type", "bitmap")
            output.addProperty("file", texture.asString())
            output.addProperty("ascent", ascent)
            output.addProperty("height", height)
            output.add("chars", JsonArray().apply { unicodes().forEach { add(it) } })
            return output
        }

        fun checkPermission(player: Player?) =
            !emojyConfig.requirePermissions || player == null || player.hasPermission(permission) || player.hasPermission(
                fontPermission
            )

        fun formattedUnicode(
            appendSpace: Boolean = false,
            insert: Boolean = true,
            colorable: Boolean = false
        ): Component {
            var bitmap = when {
                unicodes().size > 1 -> Component.textOfChildren(*unicodes().map {
                    Component.text().content(it).build().appendNewline()
                }.toTypedArray())

                else -> Component.text().content(unicodes().first()).build()
            }.font(font)

            bitmap = if (colorable) bitmap else bitmap.color(NamedTextColor.WHITE)

            bitmap = if (!insert) bitmap else bitmap.insertion(":${id}:").hoverEvent(
                hoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    ("<red>Type <i>:</i>$id<i>:</i> or <i><u>Shift + Click</i> this to use this emote").miniMsg()
                )
            )
            return if (appendSpace) bitmap.space() else bitmap
        }
    }
}

@Serializable
data class Gifs(val gifs: Set<Gif> = mutableSetOf()) {
    @Serializable
    data class Gif(
        val id: String,
        @EncodeDefault(NEVER) var frameCount: Int = 0,
        @EncodeDefault(NEVER) @SerialName("framePath") val _framePath: @Serializable(KeySerializer::class) Key = Key.key(
            "${emojyConfig.defaultNamespace}:${emojyConfig.defaultFolder}/$id"
        ),
        @EncodeDefault(NEVER) val ascent: Int = 8,
        @EncodeDefault(NEVER) val height: Int = 8,
        @EncodeDefault(NEVER) val type: GifType = GifType.SHADER
    ) {
        @Transient val framePath = Key.key(_framePath.asString().removeSuffix("/") + "/")
        @Transient val font = Key.key(framePath.namespace(), id)
        @Transient val permission = "emojy.gif.$id"
        private var aspectRatio by Delegates.notNull<Float>()

        enum class GifType {
            SHADER, OBFUSCATION
        }

        private fun unicode(index: Int): Char = Character.toChars(PRIVATE_USE_FIRST + index).first()
        private fun unicode(): String {
            return when (type) {
                GifType.SHADER -> (1..frameCount()).joinToString(unicode(frameCount() + 1).toString()) {
                    unicode(it).toString()
                }.miniMsg().font(font).color(TextColor.fromHexString("#FEFEFE")).serialize()

                GifType.OBFUSCATION -> unicode(1).toString().miniMsg()
                    .decorate(TextDecoration.OBFUSCATED).font(font).color(NamedTextColor.WHITE).serialize()
            }
        }
        fun frameCount(): Int {
            if (frameCount <= 0) frameCount = runCatching {
                val reader = ImageIO.getImageReadersByFormatName("gif").next()
                reader.input = ImageIO.createImageInputStream(gifFolder.resolve("${id}.gif"))
                aspectRatio = reader.getAspectRatio(0)
                reader.getNumImages(true)
            }.onFailure {
                if (emojyConfig.debug) logError("Could not get frame count for ${id}.gif")
            }.getOrNull() ?: 0

            return frameCount
        }

        fun toJson(): MutableList<JsonObject> {
            val jsonList = mutableListOf<JsonObject>()
            val frameCount = frameCount()
            (1..frameCount).forEach { i ->
                val output = JsonObject()
                output.addProperty("type", "bitmap")
                output.addProperty("file", "$framePath$i.png")
                output.addProperty("ascent", ascent)
                output.addProperty("height", height)
                output.add("chars", JsonArray().apply { add(unicode(i).toString()) })
                jsonList.add(output)
            }

            // Add a negative shift into the shader for ease of use
            if (type == GifType.SHADER) {
                jsonList.add(JsonObject().apply {
                    addProperty("type", "space")
                    add("advances", JsonObject().apply { addProperty(unicode(frameCount + 1).toString(), -(height * aspectRatio + 1).roundToInt()) })
                })
            }

            return jsonList
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
            return if (appendSpace) bitmap.space() else bitmap
        }
    }
}
