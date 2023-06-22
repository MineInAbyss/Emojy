package com.mineinabyss.emojy

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.emojy.EmojyGenerator.gifFolder
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.event.HoverEvent.hoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import javax.imageio.ImageIO

const val PRIVATE_USE_FIRST = 57344

// TODO Temporary way of getting default values, should be replaced with a better system
private val configFile = File(emojy.plugin.dataFolder, "config.yml")
private val configuration = YamlConfiguration.loadConfiguration(configFile)
private val defaultNamespace: String = configuration.getString("defaultNamespace", "emotes").toString()
private val defaultFolder: String = configuration.getString("defaultFolder", "emotes").toString()
private val defaultFont: String = configuration.getString("defaultFont", "emotes").toString()
private val defaultHeight: Int = configuration.getInt("defaultHeight", 8)
private val defaultAscent: Int = configuration.getInt("defaultAscent", 8)

enum class ListType {
    BOOK, CHAT
}

@Serializable
data class EmojyConfig(
    //TODO Figure out a way for these default values to be serialized and used in subclasses correctly
    /*val defaultNamespace: String = "emotes",
    val defaultFolder: String = "emotes",
    val defaultFont: String = "emotes",
    val defaultHeight: Int = 8,
    val defaultAscent: Int = 8,*/
    val requirePermissions: Boolean = true,
    val generateResourcePack: Boolean = true,
    val debug: Boolean = true,
    val listType: ListType = ListType.BOOK,
    val emotes: Set<Emote> = mutableSetOf(Emote("")),
    val gifs: Set<Gif> = mutableSetOf(Gif(""))
) {
    @Serializable
    data class Emote(
        val id: String,
        @SerialName("font")
        val _font: String = defaultFont,
        val texture: String = "${defaultNamespace}:${defaultFolder}/$id.png",
        val height: Int = defaultHeight,
        val ascent: Int = defaultAscent,
        val bitmapWidth: Int = 1,
        val bitmapHeight: Int = 1,
    ) {
        // Beginning of Private Use Area \uE000 -> uF8FF
        // Option: (Character.toCodePoint('\uE000', '\uFF8F')/37 + getIndex())
        private val lastUsedUnicode: MutableMap<String, Int> = mutableMapOf()
        fun getUnicodes(): MutableList<String> {
            return mutableListOf("").apply {
                for (i in 0 until bitmapHeight) {
                    for (j in 0 until bitmapWidth) {
                        val lastUnicode = lastUsedUnicode[_font] ?: 0
                        val row = ((getOrNull(i) ?: "") + Character.toChars(
                            PRIVATE_USE_FIRST + lastUnicode + emojy.config.emotes
                                .filter { it._font == _font }.map { it }.indexOf(this@Emote)
                        ).firstOrNull().toString())
                        if (getOrNull(i) == null)
                            add(i, row) else set(i, row)
                        lastUsedUnicode.put(_font, lastUnicode + 1) ?: lastUsedUnicode.putIfAbsent(_font, 1)
                    }
                }
                lastUsedUnicode.clear()
            }
        }


        val font get() = Key.key(namespace, _font)
        val namespace get() = texture.substringBefore(":")
        val image get() = texture.substringAfterLast("/")
        val imagePath get() = texture.substringAfter(":")
        val permission get() = "emojy.emote.$id"
        val fontPermission get() = "emojy.font.$font"
        fun toJson(): JsonObject {
            val output = JsonObject()
            val chars = JsonArray()
            output.addProperty("type", "bitmap")
            output.addProperty("file", texture)
            output.addProperty("ascent", ascent)
            output.addProperty("height", height)
            for (char in getUnicodes()) chars.add(char)
            output.add("chars", chars)
            return output
        }

        fun checkPermission(player: Player?) =
            !emojy.config.requirePermissions || player == null || player.hasPermission(permission) || player.hasPermission(fontPermission)

        fun getFormattedUnicode(splitter: String = "", insert: Boolean = true): Component {
            val stripResolver = mutableSetOf<TagResolver>()
                .apply { if (!insert) addAll(listOf(StandardTags.hoverEvent(), StandardTags.insertion())) }
            val tagResolver = TagResolver.builder().resolvers(stripResolver).build()
            val mm = MiniMessage.builder().tags(tagResolver).build()

            val bitmap = (if (getUnicodes().size > 1) {
                getUnicodes().joinToString(splitter) { "<newline>$it" }
            } else getUnicodes().first()).miniMsg()

            val component = mm.stripTags(
                bitmap.font(font).color(NamedTextColor.WHITE).insertion(":${id}:")
                    .hoverEvent(hoverEvent(HoverEvent.Action.SHOW_TEXT,
                        ("<red>Type <i>:</i>$id<i>:</i> or <i><u>Shift + Click</i> this to use this emote").miniMsg())
                    ).serialize()
            ).miniMsg()

            return if (splitter.isEmpty() || emojy.config.emotes.indexOf(this) == emojy.config.emotes.size - 1) component
            else component.append("<font:default><white>$splitter</white></font>".miniMsg())
        }
    }


    @Serializable
    data class Gif(
        val id: String,
        val frameCount: Int = 0,
        val framePath: String = "${defaultNamespace}:${defaultFolder}/$id/",
        val ascent: Int = 8,
        val height: Int = 8,
        val type: GifType = GifType.OBFUSCATION
    ) {
        enum class GifType {
            SHADER, OBFUSCATION
        }

        val font get() = Key.key(namespace, id)
        val namespace get() = framePath.substringBefore(":")
        val image get() = framePath.substringAfterLast("/")
        val imagePath get() = framePath.substringAfter(":")
        val permission get() = "emojy.gif.$id"
        fun getUnicode(i: Int): Char = Character.toChars(PRIVATE_USE_FIRST + i).first()
        fun getUnicodes(): String {
            return when (type) {
                GifType.SHADER -> (1..getFrameCount()).joinToString(getUnicode(getFrameCount() + 1).toString()) { getUnicode(it).toString() }
                    .miniMsg().font(font).color(TextColor.fromHexString("#FEFEFE")).serialize()
                GifType.OBFUSCATION -> getUnicode(1).toString().miniMsg()
                    .decorate(TextDecoration.OBFUSCATED).font(font).color(NamedTextColor.WHITE).serialize()
            }
        }

        @JvmName("getFrameCount1")
        fun getFrameCount(): Int {
            val reader = ImageIO.getImageReadersByFormatName("gif").next()
            reader.input = ImageIO.createImageInputStream(gifFolder.resolve("${id}.gif"))

            return try {
                if (frameCount <= 0) reader.getNumImages(true) else frameCount
            } catch (e: IllegalStateException) {
                logError("Could not get frame count for ${id}.gif")
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
            !emojy.config.requirePermissions || player == null || player.hasPermission(permission)

        fun getFormattedUnicode(splitter: String = "", insert: Boolean = true): Component {
            val stripResolver = mutableSetOf<TagResolver>()
                .apply { if (!insert) addAll(listOf(StandardTags.hoverEvent(), StandardTags.insertion())) }
            val tagResolver = TagResolver.builder().resolvers(stripResolver).build()
            val mm = MiniMessage.builder().tags(tagResolver).build()

            val component = mm.stripTags(getUnicodes().miniMsg().insertion(":${id}:")
                .hoverEvent(hoverEvent(HoverEvent.Action.SHOW_TEXT,
                    ("<red>Type <i>:</i>$id<i>:</i> or <i><u>Shift + Click</i> this to use this gif").miniMsg())
                ).serialize()
            ).miniMsg()

            return if (emojy.config.gifs.indexOf(this) == emojy.config.gifs.size - 1) component
            else component.append("<font:default><white>$splitter</white></font:default>".miniMsg())
        }
    }
}
