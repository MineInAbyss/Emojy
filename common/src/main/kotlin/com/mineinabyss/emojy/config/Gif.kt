package com.mineinabyss.emojy.config

import com.mineinabyss.emojy.emoteRegistry
import com.mineinabyss.emojy.hasPermission
import com.mineinabyss.emojy.spaceComponent
import com.mineinabyss.emojy.withUsageHover
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.`object`.ObjectContents

data class Gif(
    val id: String,
    var frameCount: Int = 0,
    val framePath: Key,
    val ascent: Int = 8,
    val height: Int = 8,
    var offset: Int? = null,
    val type: GifType,
) {
    val font = Key.key(framePath.namespace(), id)
    val permission = "emojy.gif.$id"
    val baseRegex = "(?<!\\\\):$id:".toRegex()
    val escapedRegex = "\\\\:$id:".toRegex()

    var aspectRatio = 0f

    enum class GifType {
        SHADER, OBFUSCATION, SPRITE
    }

    fun unicode(index: Int): String = Character.toChars(PRIVATE_USE_FIRST + index).first().toString()

    private fun unicode(): Component = when (type) {
        GifType.SHADER -> Component.text((0 until frameCount).joinToString(unicode(frameCount), transform = ::unicode))
            .font(font).color(SHADER_COLOR)

        GifType.OBFUSCATION -> Component.text(unicode(0), NamedTextColor.WHITE)
            .decorate(TextDecoration.OBFUSCATED).font(font)

        GifType.SPRITE -> Component.`object`(ObjectContents.sprite(GUI_ATLAS, framePath))
    }

    fun checkPermission(audience: Audience?) = !emoteRegistry.requirePermissions || audience.hasPermission(permission)

    fun formattedUnicode(appendSpace: Boolean = false, insert: Boolean = true): Component {
        val bitmap = Component.textOfChildren(unicode()).font(font).color(SHADER_COLOR)
            .let { if (insert) it.withUsageHover(id) else it }
        return if (appendSpace) Component.textOfChildren(bitmap, spaceComponent(2)) else bitmap
    }

    companion object {
        // The gif shader only animates text in this exact color
        val SHADER_COLOR = TextColor.fromHexString("#FEFEFE")
        val GUI_ATLAS = Key.key("minecraft", "gui")
    }
}
