package com.mineinabyss.emojy

import com.mineinabyss.emojy.nms.EmojyNMSHandlers
import com.mineinabyss.idofront.textcomponents.serialize
import io.papermc.paper.event.player.AsyncChatDecorateEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.*

@Suppress("UnstableApiUsage")
class EmojyListener : Listener {

    @EventHandler
    fun PlayerJoinEvent.injectPlayer() {
        EmojyNMSHandlers.getHandler()?.inject(player)
    }

    // Replace with result not original message to avoid borking other chat formatting
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun AsyncChatDecorateEvent.onPlayerChat() {
        result(result().replaceEmoteIds(player()))
    }
}

//TODO Tags like rainbow and gradient, which split the text into multiple children, will break replacement below
// Find out why this is called 3 times
fun Component.replaceEmoteIds(player: Player? = null, insert: Boolean = true): Component {
    var msg = GlobalTranslator.render(this, player?.locale() ?: Locale.US)
    val serialized = msg.serialize()

    emojy.config.emotes.filter { ":${it.id}:" in serialized }.forEach { emote ->
        if (emote.checkPermission(player)) {
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .match(":${emote.id}:(c|colorable):")
                    .replacement(emote.getFormattedUnicode(insert = insert, colorable = true))
                    .build()
            )
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .matchLiteral(":${emote.id}:")
                    .replacement(emote.getFormattedUnicode(insert = insert, colorable = false))
                    .build()
            )
        }
    }

    emojy.config.gifs.filter { ":${it.id}:" in serialized }.forEach { gif ->
        if (gif.checkPermission(player)) {
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .matchLiteral(":${gif.id}:")
                    .replacement(gif.getFormattedUnicode(insert = insert))
                    .build()
            )
        }
    }

    return msg
}
