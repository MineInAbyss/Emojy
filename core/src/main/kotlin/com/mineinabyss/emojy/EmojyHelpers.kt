package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.SPACE_PERMISSION
import com.mineinabyss.idofront.font.Space
import com.mineinabyss.idofront.textcomponents.serialize
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import java.util.*

//TODO Tags like rainbow and gradient, which split the text into multiple children, will break replacement below
// Above is due to Adventure-issue, nothing on our end for once. https://github.com/KyoriPowered/adventure/issues/872
// Find out why this is called 3 times
fun Component.replaceEmoteIds(player: Player? = null, insert: Boolean = true): Component {
    var msg = GlobalTranslator.render(this, player?.locale() ?: Locale.US)
    val serialized = msg.serialize()

    emojy.emotes.filter { ":${it.id}:" in serialized && it.checkPermission(player) }.forEach { emote ->
        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .match(":${emote.id}:(c|colorable):")
                .replacement(emote.formattedUnicode(insert = insert, colorable = true))
                .build()
        )
        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .matchLiteral(":${emote.id}:")
                .replacement(emote.formattedUnicode(insert = insert, colorable = false))
                .build()
        )
    }

    emojy.gifs.filter { ":${it.id}:" in serialized && it.checkPermission(player) }.forEach { gif ->
        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .matchLiteral(":${gif.id}:")
                .replacement(gif.formattedUnicode(insert = insert))
                .build()
        )
    }

    if (player?.hasPermission(SPACE_PERMISSION) != false) ":space_(-?\\d+):".toRegex().findAll(serialized)
        .mapNotNull { it.groupValues[1].toIntOrNull() }.toSet().forEach { space ->
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .matchLiteral(":space_$space:")
                    .replacement(buildSpaceComponents(space))
                    .build()
            )
        }

    return msg
}

private fun buildSpaceComponents(space: Int) =
    Component.text().content(Space.of(space)).font(emojyConfig.spaceFont).build()

fun Component.space() = append(Component.text().content(" ").font(Key.key("default")))
