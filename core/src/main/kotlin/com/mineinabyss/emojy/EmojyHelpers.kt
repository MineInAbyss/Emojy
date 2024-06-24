package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.SPACE_PERMISSION
import com.mineinabyss.idofront.font.Space
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import java.util.*

val spaceRegex: Regex = "(?<!\\\\):space_(-?\\d+):".toRegex()
val escapedSpaceRegex: Regex = "\\\\(:space_(-?\\d+):)".toRegex()
val colorableRegex: Regex = "\\|(c|colorable)".toRegex()
val bitmapIndexRegex: Regex = "\\|([0-9]+)".toRegex()

val ORIGINAL_SIGN_FRONT_LINES = NamespacedKey.fromString("emojy:original_front_lines")!!
val ORIGINAL_SIGN_BACK_LINES = NamespacedKey.fromString("emojy:original_back_lines")!!
val ORIGINAL_ITEM_RENAME_TEXT = NamespacedKey.fromString("emojy:original_item_rename")!!

fun spaceComponent(space: Int) = Component.textOfChildren(Component.text(Space.of(space)).font(emojyConfig.spaceFont))

fun Component.transformEmotes(locale: Locale? = null, insert: Boolean = false): Component {
    var component = GlobalTranslator.render(this, locale ?: Locale.US)
    val serialized = component.serialize()

    for (emote in emojy.emotes) emote.baseRegex.findAll(serialized).forEach { match ->

        val colorable = colorableRegex in match.value
        val bitmapIndex = bitmapIndexRegex.find(match.value)?.groupValues?.get(1)?.toIntOrNull() ?: -1

        component = component.replaceText(
            TextReplacementConfig.builder()
                .match(emote.baseRegex.pattern).once()
                .replacement(
                    Component.textOfChildren(emote.formattedUnicode(
                        insert = insert,
                        colorable = colorable,
                        bitmapIndex = bitmapIndex
                    ))
                )
                .build()
        )
    }

    for (gif in emojy.gifs) gif.baseRegex.findAll(serialized).forEach { _ ->
        component = component.replaceText(
            TextReplacementConfig.builder()
                .match(gif.baseRegex.pattern).once()
                .replacement(gif.formattedUnicode(insert = false))
                .build()
        )
    }

    spaceRegex.findAll(serialized).forEach { match ->
        val space = match.groupValues[1].toIntOrNull() ?: return@forEach
        val spaceRegex = "(?<!\\\\):space_(-?$space+):".toRegex()
        component = component.replaceText(
            TextReplacementConfig.builder()
                .match(spaceRegex.pattern).once()
                .replacement(spaceComponent(space))
                .build()
        )
    }

    return component
}

fun Component.escapeEmoteIDs(player: Player?): Component {
    var component = this
    val serialized = component.serialize()

    // Replace all unicodes found in default font with a random one
    // This is to prevent use of unicodes from the font the chat is in
    val (defaultKey, randomKey) = Key.key("default") to Key.key("random")
    for (emote in emojy.emotes.filter { it.font == defaultKey && !it.checkPermission(player) }) emote.unicodes.forEach {
        component = component.replaceText(
            TextReplacementConfig.builder()
                .matchLiteral(it)
                .replacement(it.miniMsg().font(randomKey))
                .build()
        )
    }

    for (emote in emojy.emotes) emote.baseRegex.findAll(serialized).forEach { match ->
        if (emote.checkPermission(player)) return@forEach

        component = component.replaceText(
            TextReplacementConfig.builder()
                .match("(?<!\\\\)${match.value}")
                .replacement(Component.text("\\${match.value}"))
                .build()
        )
    }

    for (gif in emojy.gifs) gif.baseRegex.findAll(serialized).forEach { match ->
        if (gif.checkPermission(player)) return@forEach
        component = component.replaceText(
            TextReplacementConfig.builder()
                .match("(?<!\\\\)${match.value}")
                .replacement("\\${match.value}".miniMsg())
                .build()
        )
    }

    spaceRegex.findAll(serialized).forEach { match ->
        if (player?.hasPermission(SPACE_PERMISSION) != false) return@forEach
        val space = match.groupValues[1].toIntOrNull() ?: return@forEach

        component = component.replaceText(
            TextReplacementConfig.builder()
                .match("(?<!\\\\)${match.value}")
                .replacement("\\:space_$space:".miniMsg())
                .build()
        )
    }

    return component
}

fun Component.unescapeEmoteIds(): Component {
    var component = this
    val serialized = this.serialize()

    for (emote in emojy.emotes) emote.escapedRegex.findAll(serialized).forEach { match ->
        component = component.replaceText(
            TextReplacementConfig.builder()
                .match(emote.escapedRegex.pattern).once()
                .replacement(match.value.removePrefix("\\"))
                .build()
        )
    }

    for (gif in emojy.gifs) gif.escapedRegex.findAll(serialized).forEach { match ->
        component = component.replaceText(
            TextReplacementConfig.builder()
                .match(gif.escapedRegex.pattern).once()
                .replacement(match.value.removePrefix("\\"))
                .build()
        )
    }

    escapedSpaceRegex.findAll(serialized).forEach { match ->
        component = component.replaceText(
            TextReplacementConfig.builder()
                .match(match.value).once()
                .replacement(match.value.removePrefix("\\"))
                .build()
        )
    }

    return component
}
