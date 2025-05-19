package com.mineinabyss.emojy.config

import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.emojyConfig
import com.mineinabyss.emojy.spaceComponent
import com.mineinabyss.emojy.templates
import com.mineinabyss.idofront.serialization.KeySerializer
import com.mineinabyss.idofront.textcomponents.miniMsg
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.event.HoverEvent.hoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider

@Serializable
data class Emote(
    val id: String,
    @SerialName("template") @EncodeDefault(NEVER) private val _template: String? = null,
    @EncodeDefault(NEVER) @Transient val template: EmojyTemplate? = templates.find { it.id == _template },

    @EncodeDefault(NEVER) val font: @Serializable(KeySerializer::class) Key =
        template?.font ?: emojyConfig.defaultFont,
    @SerialName("texture") @EncodeDefault(NEVER) private val _texture: @Serializable(KeySerializer::class) Key =
        template?.texture ?: Key.key("${emojyConfig.defaultNamespace}:${emojyConfig.defaultFolder}/${id.lowercase()}.png"),
    @EncodeDefault(NEVER) val height: Int = template?.height ?: emojyConfig.defaultHeight,
    @EncodeDefault(NEVER) val ascent: Int = template?.ascent ?: emojyConfig.defaultAscent,
    @EncodeDefault(NEVER) val bitmapWidth: Int = template?.bitmapWidth ?: 1,
    @EncodeDefault(NEVER) val bitmapHeight: Int = template?.bitmapHeight ?: 1,
) {
    @Transient
    val isMultiBitmap = bitmapWidth > 1 || bitmapHeight > 1
    @Transient
    val baseRegex = "(?<!\\\\):$id(\\|(c|colorable|\\d+(\\.\\.\\d+)?))*:".toRegex()
    @Transient
    val escapedRegex = "\\\\:$id(\\|(c|colorable|\\d+))*:".toRegex()
    @Transient
    val texture = Key.key(_texture.asString().removeSuffix(".png").plus(".png"))

    // Beginning of Private Use Area \uE000 -> uF8FF
    // Option: (Character.toCodePoint('\uE000', '\uFF8F')/37 + getIndex())
    @Transient
    private val lastUsedUnicode: MutableMap<Key, Int> = mutableMapOf()

    // We get this lazily so we dont need to use a function and check every time
    // but also because EmojyContext needs to be registered, so a normal val does not work
    //TODO Rework to be List<CharArray> instead
    val unicodes: MutableList<String> by lazy {
        mutableListOf("").apply {
            for (i in 0 until bitmapHeight) {
                for (j in 0 until bitmapWidth) {
                    val lastUnicode = lastUsedUnicode[font] ?: 0
                    val index = getOrNull(i)
                    val row = (index ?: "") + Character.toChars(
                        PRIVATE_USE_FIRST + lastUnicode + emojy.emotes
                            .filter { it.font == font }.indexOf(this@Emote)
                    ).firstOrNull().toString()

                    if (index == null) add(i, row) else set(i, row)
                    lastUsedUnicode[font] = lastUnicode + 1
                }
            }
            lastUsedUnicode.clear()
        }
    }

    private val permission get() = "emojy.emote.$id"
    private val fontPermission get() = "emojy.font.$font"
    val fontProvider by lazy { FontProvider.bitMap(texture, height, ascent, unicodes) }
    fun appendFont(resourcePack: ResourcePack) =
        (resourcePack.font(font)?.toBuilder() ?: Font.font().key(font)).addProvider(fontProvider).build().addTo(resourcePack)

    fun checkPermission(player: Player?) =
        !emojyConfig.requirePermissions || player?.hasPermission(permission) != false || player.hasPermission(fontPermission)

    fun replacementConfig(
        appendSpace: Boolean = false,
        insert: Boolean = true,
        colorable: Boolean = false,
        bitmapIndex: Int = -1
    ): TextReplacementConfig {
        return if (!appendSpace && insert && !colorable && bitmapIndex == -1) defaultReplacementConfig
        else TextReplacementConfig.builder().match(baseRegex.pattern).once().replacement(formattedComponent(appendSpace, insert, colorable, bitmapIndex)).build()
    }
    private val defaultReplacementConfig by lazy {
        TextReplacementConfig.builder().match(baseRegex.pattern).once().replacement(defaultComponent).build()
    }
    private val defaultComponent by lazy { formattedComponent() }
    fun formattedComponent(
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

        return if (appendSpace) bitmap.append(spaceComponent(2)) else bitmap
    }
}