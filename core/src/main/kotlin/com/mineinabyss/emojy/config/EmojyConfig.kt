@file:OptIn(ExperimentalSerializationApi::class)

package com.mineinabyss.emojy.config

import co.touchlab.kermit.Severity
import com.mineinabyss.emojy.*
import com.mineinabyss.idofront.serialization.KeySerializer
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import kotlinx.serialization.*
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.event.HoverEvent.hoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
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
    val defaultHeight: Int = 7,
    val defaultAscent: Int = 7,
    val spaceFont: @Serializable(KeySerializer::class) Key = Key.key("minecraft:space"),

    val requirePermissions: Boolean = true,
    val supportForceUnicode: Boolean = true,
    val logLevel: Severity = Severity.Debug,
    val emojyList: EmojyList = EmojyList(),
    val supportedLanguages: Set<String> = mutableSetOf("en_us"),
) {

    @Serializable
    data class EmojyList(
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
            template?.texture
                ?: Key.key("${emojyConfig.defaultNamespace}:${emojyConfig.defaultFolder}/${id.lowercase()}.png"),
        @EncodeDefault(NEVER) val height: Int = template?.height ?: emojyConfig.defaultHeight,
        @EncodeDefault(NEVER) val ascent: Int = template?.ascent ?: emojyConfig.defaultAscent,
        @EncodeDefault(NEVER) val bitmapWidth: Int = template?.bitmapWidth ?: 1,
        @EncodeDefault(NEVER) val bitmapHeight: Int = template?.bitmapHeight ?: 1,
    ) {
        val isMultiBitmap: Boolean get() = bitmapWidth > 1 || bitmapHeight > 1
        @Transient
        val baseRegex = "(?<!\\\\):$id(\\|(c|colorable|\\d+))*:".toRegex()
        @Transient
        val escapedRegex = "\\\\:$id(\\|(c|colorable|\\d+))*:".toRegex()

        // Beginning of Private Use Area \uE000 -> uF8FF
        // Option: (Character.toCodePoint('\uE000', '\uFF8F')/37 + getIndex())
        @Transient
        private val lastUsedUnicode: MutableMap<Key, Int> = mutableMapOf()

        // We get this lazily so we dont need to use a function and check every time
        // but also because EmojyContext needs to be registered, so a normal vbal does not work
        //TODO Rework to be List<CharArray> instead
        val unicodes: MutableList<String> by lazy {
            mutableListOf("").apply {
                for (i in 0 until bitmapHeight) {
                    for (j in 0 until bitmapWidth) {
                        val lastUnicode = lastUsedUnicode[font] ?: 0
                        val row = ((getOrNull(i) ?: "") + Character.toChars(
                            PRIVATE_USE_FIRST + lastUnicode + emojy.emotes
                                .filter { it.font == font }.map { it }.indexOf(this@Emote)
                        ).firstOrNull().toString())
                        if (getOrNull(i) == null)
                            add(i, row) else set(i, row)
                        lastUsedUnicode[font] = lastUnicode + 1
                    }
                }
                lastUsedUnicode.clear()
            }
        }

        private val permission get() = "emojy.emote.$id"
        private val fontPermission get() = "emojy.font.$font"
        private fun fontProvider() = FontProvider.bitMap(texture, height, ascent, unicodes)
        fun appendFont(resourcePack: ResourcePack) =
            (resourcePack.font(font)?.toBuilder() ?: Font.font().key(font)).addProvider(fontProvider()).build()

        fun checkPermission(player: Player?) =
            !emojyConfig.requirePermissions || player == null || player.hasPermission(permission) || player.hasPermission(
                fontPermission
            )

        fun formattedUnicode(
            appendSpace: Boolean = false,
            insert: Boolean = true,
            colorable: Boolean = false,
            bitmapIndex: Int = -1
        ): Component {
            var bitmap = when {
                unicodes.map { it.map(Char::toString) }.flatten().size > 1 -> {
                    if (bitmapIndex >= 0) {
                        val unicode = unicodes.joinToString("").toCharArray()
                            .getOrElse(maxOf(bitmapIndex, 1) - 1) { unicodes.last().last() }.toString()
                        Component.text(unicode).font(font)
                    } else Component.textOfChildren(*unicodes.map {
                        listOf(
                            Component.text(it).font(font),
                            spaceComponent(-1)
                        )
                    }.flatten().toTypedArray())

                }

                else -> Component.text(unicodes.first().first().toString()).font(font)
            }

            bitmap = if (colorable) bitmap else bitmap.color(NamedTextColor.WHITE)

            bitmap = if (!insert) bitmap else bitmap.insertion(":${id}:").hoverEvent(
                hoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    ("<red>Type <i>:</i>$id<i>:</i> or <i><u>Shift + Click</i> this to use this emote").miniMsg()
                )
            )

            if (appendSpace) bitmap.append(spaceComponent(2))

            return bitmap
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
        @Transient
        val framePath = Key.key(_framePath.asString().removeSuffix("/") + "/")
        @Transient
        val font = Key.key(framePath.namespace(), id)
        @Transient
        val permission = "emojy.gif.$id"
        @Transient
        val baseRegex = "(?<!\\\\):$id:".toRegex()
        @Transient
        val escapedRegex = "\\\\:$id:".toRegex()

        val gifFile get() = emojy.plugin.dataFolder.resolve("gifs/${id}.gif").apply { mkdirs() }
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
                reader.input = ImageIO.createImageInputStream(gifFile)
                aspectRatio = reader.getAspectRatio(0)
                reader.getNumImages(true)
            }.onFailure {
                emojy.logger.d("Could not get frame count for ${id}.gif")
            }.getOrNull() ?: run {
                aspectRatio = 1f
                0
            }

            return frameCount
        }

        fun font() = Font.font(font, fontProviders().toMutableList().also { it.add(gifAdvance()) })
        private fun gifAdvance() =
            FontProvider.space().advance(unicode(frameCount() + 1).toString(), -(height * aspectRatio + 1).roundToInt())
                .build()

        private fun fontProviders(): List<FontProvider> = (1..frameCount()).map {
            FontProvider.bitMap(
                Key.key("$framePath$it.png"),
                height,
                ascent,
                listOf(unicode(it).toString())
            )
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
    }
}
