package com.mineinabyss.emojy

import com.mineinabyss.idofront.messaging.*
import io.papermc.paper.event.player.AsyncChatCommandDecorateEvent
import io.papermc.paper.event.player.AsyncChatDecorateEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerEditBookEvent

@Suppress("UnstableApiUsage")
class EmojyListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun AsyncChatCommandDecorateEvent.onCommand() {
        result(originalMessage().replaceEmoteIds(player()))
    }

    // Replace with result not original message to avoid borking other chat formatting
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun AsyncChatDecorateEvent.onPlayerChat() {
        result(result().replaceEmoteIds(player()))
    }

    //TODO Discard this for packet based later
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerEditBookEvent.onPlayerWriteBook() {
        newBookMeta = newBookMeta.apply {
            pages(pages().map { page -> page.replaceEmoteIds(player) })
        }
    }

    //TODO Discard this for packet based later
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun SignChangeEvent.onSignChange() = lines().forEachIndexed { i, l -> line(i, l.replaceEmoteIds(player)) }
}

fun Component.replaceEmoteIds(player: Player? = null, insert: Boolean = true): Component {
    var msg = this
    emojyConfig.emotes.forEach { emote ->
        if (emote.checkPermission(player)) {
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .match(":${emote.id}:")
                    .replacement(emote.getFormattedUnicode(insert = insert))
                    .build()
            )
        }
    }
    emojyConfig.gifs.forEach { gif ->
        if (gif.checkPermission(player)) {
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .match(":${gif.id}:")
                    .replacement(gif.getFormattedUnicode())
                    .build()
            )
        }
    }
    return msg
}
