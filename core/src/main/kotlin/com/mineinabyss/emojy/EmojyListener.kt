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
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

@Suppress("UnstableApiUsage")
class EmojyListener : Listener {

    @EventHandler
    fun PlayerJoinEvent.injectPlayer() {
        EmojyNMSHandlers.getHandler()?.inject(player)
    }

    @EventHandler
    fun PlayerQuitEvent.uninjectPlayer() {
        EmojyNMSHandlers.getHandler()?.uninject(player)
    }

    // Replace with result not original message to avoid borking other chat formatting
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun AsyncChatDecorateEvent.onPlayerChat() {
        result(result().replaceEmoteIds(player()))
    }
}

//TODO Tags like rainbow and gradient, which split the text into multiple children, will break replacement below
// Find out why this is called 3 times
fun Component.replaceEmoteIds(player: Player? = null, insert: Boolean = true): Component {
    var msg = GlobalTranslator.render(this, player?.locale() ?: Locale.US)
    val serialized = msg.serialize()

    emojy.config.emotes.firstOrNull { ":${it.id}:" in serialized }?.let { emote ->
        if (emote.checkPermission(player)) {
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .matchLiteral(":${emote.id}:")
                    .replacement(emote.getFormattedUnicode(insert = insert))
                    .build()
            )
        }
    }

    emojy.config.gifs.firstOrNull { ":${it.id}:" in serialized }?.let { gif ->
        if (gif.checkPermission(player)) {
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .match(":${gif.id}:")
                    .replacement(gif.getFormattedUnicode(insert = insert))
                    .build()
            )
        }
    }

    return msg
}
