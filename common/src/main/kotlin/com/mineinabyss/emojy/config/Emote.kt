package com.mineinabyss.emojy.config

import com.mineinabyss.emojy.emoteRegistry
import com.mineinabyss.emojy.hasPermission
import com.mineinabyss.emojy.spaceComponent
import com.mineinabyss.emojy.withUsageHover
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.format.NamedTextColor

const val PRIVATE_USE_FIRST = 57344
const val SPACE_PERMISSION = "emojy.space"

data class Emote(
    val id: String,
    val font: Key,
    val atlas: Key?,
    val texture: Key,
    val height: Int,
    val ascent: Int,
    val bitmapWidth: Int,
    val bitmapHeight: Int,
    val unicodes: List<String>,
) {
    val isMultiBitmap = bitmapWidth > 1 || bitmapHeight > 1
    val baseRegex = "(?<!\\\\):$id(\\|(c|colorable|\\d+(\\.\\.\\d+)?))*:".toRegex()
    val escapedRegex = "\\\\:$id(\\|(c|colorable|\\d+))*:".toRegex()

    val spriteKey get() = Key.key(texture.asString().removeSuffix(".png"))

    private val permission get() = "emojy.emote.$id"
    private val fontPermission get() = "emojy.font.$font"

    fun checkPermission(audience: Audience?) =
        !emoteRegistry.requirePermissions || audience.hasPermission(permission) || audience.hasPermission(fontPermission)

    fun replacementConfig(
        appendSpace: Boolean = false,
        insert: Boolean = true,
        colorable: Boolean = false,
        bitmapIndex: Int = -1,
    ): TextReplacementConfig =
        if (!appendSpace && insert && !colorable && bitmapIndex == -1) defaultReplacementConfig
        else TextReplacementConfig.builder().match(baseRegex.pattern).once().replacement(formattedComponent(appendSpace, insert, colorable, bitmapIndex)).build()

    private val defaultReplacementConfig by lazy {
        TextReplacementConfig.builder().match(baseRegex.pattern).once().replacement(defaultComponent).build()
    }
    private val defaultComponent by lazy { formattedComponent() }
    fun formattedComponent(
        appendSpace: Boolean = false,
        insert: Boolean = true,
        colorable: Boolean = false,
        bitmapIndex: Int = -1,
    ): Component {
        val characters = unicodes.joinToString("")
        val bitmap = when {
            characters.length <= 1 -> Component.text(characters.first().toString()).font(font)
            bitmapIndex >= 0 -> Component.text(characters.getOrElse(maxOf(bitmapIndex, 1) - 1) { characters.last() }.toString()).font(font)
            else -> Component.textOfChildren(*unicodes.flatMap { listOf(Component.text(it).font(font), spaceComponent(-1)) }.toTypedArray())
        }
            .let { if (colorable) it else it.color(NamedTextColor.WHITE) }
            .let { if (insert) it.withUsageHover(id) else it }

        return if (appendSpace) bitmap.append(spaceComponent(2)) else bitmap
    }

    companion object {
        /**
         * @param fontIndex position of the emote among all emotes sharing its font
         */
        fun unicodes(fontIndex: Int, bitmapWidth: Int, bitmapHeight: Int): List<String> = List(bitmapHeight) { row ->
            String(CharArray(bitmapWidth) { column ->
                Character.toChars(PRIVATE_USE_FIRST + fontIndex + row * bitmapWidth + column).first()
            })
        }
    }
}
