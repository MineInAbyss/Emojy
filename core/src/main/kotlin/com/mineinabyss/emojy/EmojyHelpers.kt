package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.SPACE_PERMISSION
import com.mineinabyss.idofront.font.Space
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import java.util.*

val spaceRegex: Regex = "(?<!\\\\):space_(-?\\d+):".toRegex()
val escapedSpaceRegex: Regex = "\\\\(:space_(-?\\d+):)".toRegex()
val colorableRegex: Regex = "\\|(c|colorable)".toRegex()
val bitmapIndexRegex: Regex = "\\|([0-9]+)".toRegex()

private val randomKey = Key.key("random")
private val randomComponent = Component.text("random").font(randomKey)
private val defaultKey = Key.key("default")
private val defaultEmoteReplacementConfigs = emojy.emotes.filter { it.font == defaultKey }.flatMap { it.unicodes }
    .map { TextReplacementConfig.builder().match(it).replacement(randomComponent).build() }

val ORIGINAL_SIGN_FRONT_LINES = NamespacedKey.fromString("emojy:original_front_lines")!!
val ORIGINAL_SIGN_BACK_LINES = NamespacedKey.fromString("emojy:original_back_lines")!!
val ORIGINAL_ITEM_RENAME_TEXT = NamespacedKey.fromString("emojy:original_item_rename")!!

fun spaceString(space: Int) = "<font:${emojyConfig.spaceFont.asMinimalString()}>${Space.of(space)}</font>"
fun spaceComponent(space: Int) = Component.textOfChildren(Component.text(Space.of(space)).font(emojyConfig.spaceFont))

val SPACE_REPLACEMENT_CONFIG = TextReplacementConfig.builder()
    .match(spaceRegex.pattern)
    .replacement { matchResult, _ ->
        spaceComponent(matchResult.group(1).toIntOrNull() ?: return@replacement null)
    }.build()

private fun Component.asFlatTextContent(): String {
    var flattened = ""
    val flatText = (this@asFlatTextContent as? TextComponent) ?: return flattened
    flattened += flatText.content()
    flattened += flatText.children().joinToString("") { it.asFlatTextContent() }
    (flatText.hoverEvent()?.value() as? Component)?.let { hover ->
        val hoverText = hover as? TextComponent ?: return@let
        flattened += hoverText.content()
        flattened += hoverText.children().joinToString("") { it.asFlatTextContent() }
    }

    return flattened
}

fun String.transformEmotes(insert: Boolean = false): String {
    var content = this

    for (emote in emojy.emotes) emote.baseRegex.findAll(this).forEach { match ->

        val colorable = colorableRegex in match.value
        val bitmapIndex = bitmapIndexRegex.find(match.value)?.groupValues?.get(1)?.toIntOrNull() ?: -1

        content = content.replaceFirst(
            emote.baseRegex, emote.formattedUnicode(
                insert = insert,
                colorable = colorable,
                bitmapIndex = bitmapIndex
            ).serialize()
        )
    }

    for (gif in emojy.gifs) gif.baseRegex.findAll(this).forEach { _ ->
        content = content.replaceFirst(gif.baseRegex, gif.formattedUnicode(insert = insert).serialize())
    }

    spaceRegex.findAll(this).forEach { match ->
        val space = match.groupValues[1].toIntOrNull() ?: return@forEach
        val spaceRegex = "(?<!\\\\):space_(-?$space+):".toRegex()

        content = content.replaceFirst(spaceRegex, spaceString(space))
    }

    return content
}

fun Component.transformEmotes(locale: Locale? = null, insert: Boolean = false): Component {
    var component = GlobalTranslator.render(this, locale ?: Locale.US)
    val serialized = component.asFlatTextContent()

    for (emote in emojy.emotes) emote.baseRegex.findAll(serialized).forEach { match ->

        val colorable = colorableRegex in match.value
        val bitmapIndex = bitmapIndexRegex.find(match.value)?.groupValues?.get(1)?.toIntOrNull() ?: -1

        component = component.replaceText(
            TextReplacementConfig.builder()
                .match(emote.baseRegex.pattern).once()
                .replacement(
                    Component.textOfChildren(
                        emote.formattedUnicode(
                            insert = insert,
                            colorable = colorable,
                            bitmapIndex = bitmapIndex
                        )
                    )
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


    component = component.replaceText(SPACE_REPLACEMENT_CONFIG)

    return component
}

fun String.escapeEmoteIDs(player: Player?): String {
    var content = this
    for (emote in emojy.emotes.filter { it.font == defaultKey && !it.checkPermission(player) }) emote.unicodes.forEach {
        content = content.replaceFirst(it, "<font:random>$it</font>")
    }

    for (emote in emojy.emotes) emote.baseRegex.findAll(this).forEach { match ->
        if (emote.checkPermission(player)) return@forEach

        content = content.replaceFirst("(?<!\\\\)${match.value}", "\\${match.value}")
    }

    for (gif in emojy.gifs) gif.baseRegex.findAll(this).forEach { match ->
        if (gif.checkPermission(player)) return@forEach
        content = content.replaceFirst("(?<!\\\\)${match.value}", "\\${match.value}")
    }

    spaceRegex.findAll(this).forEach { match ->
        if (player?.hasPermission(SPACE_PERMISSION) != false) return@forEach
        val space = match.groupValues[1].toIntOrNull() ?: return@forEach

        content = content.replaceFirst("(?<!\\\\)${match.value}", "\\:space_$space:")
    }

    return content
}

fun Component.escapeEmoteIDs(player: Player?): Component {
    var component = this
    val serialized = component.asFlatTextContent()

    // Replace all unicodes found in default font with a random one
    // This is to prevent use of unicodes from the font the chat is in
    for (defaultConfig in defaultEmoteReplacementConfigs) component = component.replaceText(defaultConfig)

    for (emote in emojy.emotes) for (match in emote.baseRegex.findAll(serialized)) {
        if (emote.checkPermission(player)) continue

        component = component.replaceText(
            TextReplacementConfig.builder()
                .match("(?<!\\\\)${match.value}")
                .replacement(Component.text("\\${match.value}"))
                .build()
        )
    }

    for (gif in emojy.gifs) for (match in gif.baseRegex.findAll(serialized)) {
        if (gif.checkPermission(player)) continue
        component = component.replaceText(
            TextReplacementConfig.builder()
                .match("(?<!\\\\)${match.value}")
                .replacement("\\${match.value}".miniMsg())
                .build()
        )
    }

    for (match in spaceRegex.findAll(serialized)) {
        if (player?.hasPermission(SPACE_PERMISSION) != false) continue
        val space = match.groupValues[1].toIntOrNull() ?: continue

        component = component.replaceText(
            TextReplacementConfig.builder()
                .match("(?<!\\\\)${match.value}")
                .replacement("\\:space_$space:".miniMsg())
                .build()
        )
    }

    return component
}

fun String.unescapeEmoteIds(): String {
    var content = this

    for (emote in emojy.emotes) emote.escapedRegex.findAll(this).forEach { match ->
        content = content.replaceFirst(emote.escapedRegex, match.value.removePrefix("\\"))
    }

    for (gif in emojy.gifs) gif.escapedRegex.findAll(this).forEach { match ->
        content = content.replaceFirst(gif.escapedRegex, match.value.removePrefix("\\"))
    }

    escapedSpaceRegex.findAll(this).forEach { match ->
        content = content.replaceFirst(match.value, match.value.removePrefix("\\"))
    }

    return content
}

fun Component.unescapeEmoteIds(): Component {
    var component = this
    val serialized = component.asFlatTextContent()

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
