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
import net.kyori.adventure.text.event.HoverEvent.hoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style.Merge
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.ImageReader

val emojyConfig get() = emojy.config.data
const val PRIVATE_USE_FIRST = 57344

// TODO Temporary way of getting default values, should be replaced with a better system
private val configFile = File(emojy.dataFolder, "config.yml")
private val configuration = YamlConfiguration.loadConfiguration(configFile)
private val defaultNamespace: String = configuration.getString("defaultNamespace", "emotes").toString()
private val defaultFolder: String = configuration.getString("defaultFolder", "emotes").toString()
private val defaultFont: String = configuration.getString("defaultFont", "emotes").toString()
private val defaultHeight: Int = configuration.getInt("defaultHeight", 8)
private val defaultAscent: Int = configuration.getInt("defaultHeight", 8)

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
    val emotes: Set<Emote> = mutableSetOf(Emote("")),
    val gifs: Set<Gif> = mutableSetOf(Gif(""))
) {
    @Serializable
    data class Emote(
        val id: String,
        val font: String = defaultFont,
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
                        val lastUnicode = lastUsedUnicode[font] ?: 0
                        val row = ((getOrNull(i) ?: "") + Character.toChars(
                            PRIVATE_USE_FIRST + lastUnicode + emojyConfig.emotes
                                .filter { it.font == font }.map { it }.indexOf(this@Emote)
                        ).firstOrNull().toString())
                        if (getOrNull(i) == null)
                            add(i, row) else set(i, row)
                        lastUsedUnicode.put(font, lastUnicode + 1) ?: lastUsedUnicode.putIfAbsent(font, 1)
                    }
                }
            }
        }


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
            for (char in getUnicodes()) chars.add(char)
            output.add("chars", chars)
            return output
        }

        fun checkPermission(player: Player?) =
            !emojyConfig.requirePermissions || player == null || player.hasPermission(getPermission()) || player.hasPermission(
                "emojy.font.${font}"
            )

        // TODO Change this to miniMsg(TagResolver) when Idofront is updated
        fun getFormattedUnicode(splitter: String = "", insert: Boolean = true): Component {
            val merges = mutableSetOf(Merge.FONT, Merge.DECORATIONS, Merge.COLOR)
            if (insert) merges.addAll(listOf(Merge.EVENTS, Merge.INSERTION))

            val bitmap = if (getUnicodes().size > 1) {
                getUnicodes().joinToString(splitter) { "<newline>$it" }
            } else getUnicodes().first()
            val component = bitmap.miniMsg().mergeStyle(
                Component.empty().font(getFont()).color(NamedTextColor.WHITE).insertion(":${id}:")
                    .hoverEvent(
                        hoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            ("<red>Type <i>:$id:</i> or <i>Shift + Click</i> this to use this emote").miniMsg()
                        )
                    ), merges
            )
            return if (splitter.isEmpty() || emojyConfig.emotes.indexOf(this) == emojyConfig.emotes.size - 1) component
            else component.append("<font:default><white>$splitter</white></font>".miniMsg())
        }

        private val emojyTagResolver: TagResolver
            get() = TagResolver.builder().apply {
                emojyConfig.emotes.forEach { emote ->
                    Placeholder.component("emojy_$id", emote.getFormattedUnicode())
                }
            }.build()
    }


    @Serializable
    data class Gif(
        val id: String,
        val frameCount: Int = 0,
        val framePath: String = "${defaultNamespace}:${defaultFolder}/$id/",
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
                        hoverEvent(
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
