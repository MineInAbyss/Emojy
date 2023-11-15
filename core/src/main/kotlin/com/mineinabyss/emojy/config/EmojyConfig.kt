@file:OptIn(ExperimentalSerializationApi::class)

package com.mineinabyss.emojy.config

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.emojy.EmojyGenerator.gifFolder
import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.emojyConfig
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
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import java.io.File
import javax.imageio.ImageIO

const val PRIVATE_USE_FIRST = 57344
const val SPACE_PERMISSION = "emojy.space"

@Serializable
data class EmojyConfig(
    val defaultNamespace: String = "emotes",
    val defaultFolder: String = "emotes",
    val defaultFont: @Serializable(KeySerializer::class) Key = Key.key("emotes:emotes"),
    val defaultHeight: Int = 8,
    val defaultAscent: Int = 8,
    val spaceFont: String = "space",

    val requirePermissions: Boolean = true,
    val generateResourcePack: Boolean = true,
    val supportForceUnicode: Boolean = true,
    val debug: Boolean = true,
    val emojyList: EmojyList = EmojyList(),
    val supportedLanguages: Set<String> = mutableSetOf("en_us"),

    ) {

    enum class ListType {
        BOOK, BOOK2, CHAT
    }
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

        @EncodeDefault(NEVER) val font: @Serializable(KeySerializer::class) Key = template?.font ?: emojyConfig.defaultFont,
        @EncodeDefault(NEVER) val texture: @Serializable(KeySerializer::class) Key = template?.texture
            ?: Key.key("${emojyConfig.defaultNamespace}:${emojyConfig.defaultFolder}/${id.lowercase()}.png"),
        @EncodeDefault(NEVER) val height: Int = template?.height ?: emojyConfig.defaultHeight,
        @EncodeDefault(NEVER) val ascent: Int = template?.ascent ?: emojyConfig.defaultAscent,
        @EncodeDefault(NEVER) val bitmapWidth: Int = template?.bitmapWidth ?: 1,
        @EncodeDefault(NEVER) val bitmapHeight: Int = template?.bitmapHeight ?: 1,
    ) {
        // Beginning of Private Use Area \uE000 -> uF8FF
        // Option: (Character.toCodePoint('\uE000', '\uFF8F')/37 + getIndex())
        @EncodeDefault(NEVER) @Transient
        private val lastUsedUnicode: MutableMap<Key, Int> = mutableMapOf()
        fun getUnicodes(): MutableList<String> {
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


        val namespace get() = texture.namespace()
        val image get() = texture.value().substringAfterLast("/")
        val imagePath get() = texture.value()
        val permission get() = "emojy.emote.$id"
        val fontPermission get() = "emojy.font.$font"
        fun toJson(): JsonObject {
            val output = JsonObject()
            val chars = JsonArray()
            output.addProperty("type", "bitmap")
            output.addProperty("file", texture.asString())
            output.addProperty("ascent", ascent)
            output.addProperty("height", height)
            for (char in getUnicodes()) chars.add(char)
            output.add("chars", chars)
            return output
        }

        fun checkPermission(player: Player?) =
            !emojyConfig.requirePermissions || player == null || player.hasPermission(permission) || player.hasPermission(
                fontPermission
            )

        fun getFormattedUnicode(
            appendSpace: Boolean = false,
            insert: Boolean = true,
            colorable: Boolean = false
        ): Component {
            var bitmap = when {
                getUnicodes().size > 1 -> Component.textOfChildren(*getUnicodes().map {
                    Component.text().content(it).build().appendNewline()
                }.toTypedArray())

                else -> Component.text().content(getUnicodes().first()).build()
            }.font(font)

            bitmap = if (colorable) bitmap else bitmap.color(NamedTextColor.WHITE)

            bitmap = if (!insert) bitmap else bitmap.insertion(":${id}:").hoverEvent(
                hoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    ("<red>Type <i>:</i>$id<i>:</i> or <i><u>Shift + Click</i> this to use this emote").miniMsg()
                )
            )
            return if (appendSpace) bitmap.apply { appendSpace().style(Style.empty()) } else bitmap
        }
    }
}

@Serializable
data class Gifs(val gifs: Set<Gif> = mutableSetOf()) {
    @Serializable
    data class Gif(
        val id: String,
        @EncodeDefault(NEVER) val frameCount: Int = 0,
        @EncodeDefault(NEVER) @SerialName("framePath") val _framePath: String = "${emojyConfig.defaultNamespace}:${emojyConfig.defaultFolder}/$id/",
        @EncodeDefault(NEVER) val ascent: Int = 8,
        @EncodeDefault(NEVER) val height: Int = 8,
        @EncodeDefault(NEVER) val type: GifType = GifType.SHADER
    ) {
        enum class GifType {
            SHADER, OBFUSCATION
        }

        val framePath get() = _framePath.let { if (it.endsWith("/")) it else "$it/" }
        val font get() = Key.key(namespace, id)
        val namespace get() = framePath.substringBefore(":")
        val image get() = framePath.substringAfterLast("/")
        val imagePath get() = framePath.substringAfter(":")
        val permission get() = "emojy.gif.$id"
        fun getUnicode(i: Int): Char = Character.toChars(PRIVATE_USE_FIRST + i).first()
        fun getUnicodes(): String {
            return when (type) {
                GifType.SHADER -> (1..getFrameCount()).joinToString(getUnicode(getFrameCount() + 1).toString()) {
                    getUnicode(it).toString()
                }.miniMsg().font(font).color(TextColor.fromHexString("#FEFEFE")).serialize()

                GifType.OBFUSCATION -> getUnicode(1).toString().miniMsg()
                    .decorate(TextDecoration.OBFUSCATED).font(font).color(NamedTextColor.WHITE).serialize()
            }
        }

        @JvmName("getFrameCount1")
        fun getFrameCount(): Int {
            val reader = ImageIO.getImageReadersByFormatName("gif").next()
            reader.input = ImageIO.createImageInputStream(gifFolder.resolve("${id}.gif"))
            File("").absoluteFile.path

            return try {
                if (frameCount <= 0) reader.getNumImages(true) else frameCount
            } catch (e: IllegalStateException) {
                if (emojyConfig.debug) logError("Could not get frame count for ${id}.gif")
                return 0
            }
        }

        fun toJson(): MutableList<JsonObject> {
            val jsonList = mutableListOf<JsonObject>()
            val frameCount = getFrameCount()
            (1..frameCount).forEach { i ->
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

            // Add a negative shift into the shader for ease of use
            if (type == GifType.SHADER) {
                val output = JsonObject()
                val advances = JsonObject()
                output.addProperty("type", "space")
                advances.addProperty(getUnicode(frameCount + 1).toString(), -9)
                output.add("advances", advances)
                jsonList.add(output)
            }

            return jsonList
        }

        fun checkPermission(player: Player?) =
            !emojyConfig.requirePermissions || player == null || player.hasPermission(permission)

        fun getFormattedUnicode(appendSpace: Boolean = false, insert: Boolean = true): Component {
            var bitmap = getUnicodes().miniMsg().font(font).color(TextColor.fromHexString("#FEFEFE"))

            bitmap = if (!insert) bitmap else bitmap.insertion(":${id}:").hoverEvent(
                hoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    ("<red>Type <i>:</i>$id<i>:</i> or <i><u>Shift + Click</i> this to use this emote").miniMsg()
                )
            )
            return if (appendSpace) bitmap.apply { appendSpace().style(Style.empty()) } else bitmap
        }
    }
}
