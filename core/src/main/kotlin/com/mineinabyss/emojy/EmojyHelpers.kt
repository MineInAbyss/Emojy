package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.SPACE_PERMISSION
import com.mineinabyss.idofront.font.Space
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import java.util.*

fun Component.transform(player: Player?, insert: Boolean, unescape: Boolean = true) = player?.let { escapeEmoteIDs(it) } ?: transformEmoteIDs(player, insert, unescape)

val legacy = LegacyComponentSerializer.legacySection()
private val spaceRegex: Regex = "(?<!\\\\):space_(-?\\d+):".toRegex()
private val escapedSpaceRegex: Regex = "\\\\(:space_(-?\\d+):)".toRegex()
private val colorableRegex: Regex = "\\|(c|colorable)".toRegex()
private val bitmapIndexRegex: Regex = "\\|([0-9]+)".toRegex()
//TODO Tags like rainbow and gradient, which split the text into multiple children, will break replacement below
// Above is due to Adventure-issue, nothing on our end for once. https://github.com/KyoriPowered/adventure/issues/872
// Find out why this is called 3 times
fun Component.escapeEmoteIDs(player: Player?): Component {
    var msg = this
    val serialized = msg.serialize()

    // Replace all unicodes found in default font with a random one
    // This is to prevent use of unicodes from the font the chat is in
    val (defaultKey, randomKey) = Key.key("default") to Key.key("random")
    for (emote in emojy.emotes.filter { it.font == defaultKey && !it.checkPermission(player) }) emote.unicodes.forEach {
        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .matchLiteral(it)
                .replacement(it.miniMsg().font(randomKey))
                .build()
        )
    }

    for (emote in emojy.emotes) emote.baseRegex.findAll(serialized).forEach { match ->
        if (emote.checkPermission(player)) return@forEach

        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .matchLiteral(match.value).once()
                .replacement("\\${match.value}".miniMsg())
                .build()
        )
    }

    for (gif in emojy.gifs) gif.baseRegex.findAll(serialized).forEach { match ->
        if (gif.checkPermission(player)) return@forEach
        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .matchLiteral(match.value).once()
                .replacement("\\${match.value}".miniMsg())
                .build()
        )
    }

    spaceRegex.findAll(serialized).forEach { match ->
        if (player?.hasPermission(SPACE_PERMISSION) != false) return@forEach
        val space = match.groupValues[1].toIntOrNull() ?: return@forEach

        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .matchLiteral(match.value).once()
                .replacement("\\:space_$space:".miniMsg())
                .build()
        )
    }

    return msg
}

/**
 * Formats emote-ids in a component to their unicode representation, ignoring escaped emote-ids.
 * This is because we handle with a player-context first, and escape that in-which should not be formatted.
 */
fun Component.transformEmoteIDs(player: Player?, insert: Boolean = true, unescape: Boolean): Component {
    var msg = GlobalTranslator.render(this, player?.locale() ?: Locale.US)
    val serialized = this.serialize()

    for (emote in emojy.emotes) {
        emote.baseRegex.findAll(serialized).forEach { match ->

            val colorable = colorableRegex in match.value
            val bitmapIndex = bitmapIndexRegex.find(match.value)?.groupValues?.get(1)?.toIntOrNull() ?: -1

            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .match(match.value).once()
                    .replacement(emote.formattedUnicode(insert = false, colorable = colorable, bitmapIndex = bitmapIndex))
                    .build()
            )
        }

        if (unescape) emote.escapedRegex.findAll(serialized).forEach { match ->
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .matchLiteral(match.value).once()
                    .replacement(match.value.removePrefix("\\"))
                    .build()
            )
        }
    }

    for (gif in emojy.gifs) {
        gif.baseRegex.findAll(serialized).forEach { match ->
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .match(gif.baseRegex.pattern)
                    .replacement(gif.formattedUnicode(insert = insert))
                    .build()
            )
        }

        if (unescape) gif.escapedRegex.findAll(serialized).forEach { match ->
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .match(gif.escapedRegex.pattern)
                    .replacement(match.value.removePrefix("\\"))
                    .build()
            )
        }
    }

    spaceRegex.findAll(serialized).forEach { match ->
        val space = match.groupValues[1].toIntOrNull() ?: return@forEach
        val spaceRegex = "(?<!\\\\):space_(-?$space+):".toRegex()
        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .match(spaceRegex.pattern)
                .replacement(spaceComponent(space))
                .build()
        )
    }

    if (unescape) escapedSpaceRegex.findAll(serialized).forEach { match ->
        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .match(match.value)
                .replacement(match.value.removePrefix("\\"))
                .build()
        )
    }

    return msg
}

fun spaceComponent(space: Int) = Component.textOfChildren(Component.text(Space.of(space)).font(emojyConfig.spaceFont))
