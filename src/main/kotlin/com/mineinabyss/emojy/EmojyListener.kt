package com.mineinabyss.emojy

import com.mineinabyss.idofront.entities.rightClicked
import com.mineinabyss.idofront.messaging.*
import com.mineinabyss.idofront.textcomponents.serialize
import io.papermc.paper.event.player.AsyncChatCommandDecorateEvent
import io.papermc.paper.event.player.AsyncChatDecorateEvent
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.meta.BookMeta

@Suppress("UnstableApiUsage")
class EmojyListener : Listener {

    @EventHandler
    fun PlayerJoinEvent.injectPlayer() {
        EmojyNMSHandler.inject(player)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun AsyncChatCommandDecorateEvent.onCommand() {
        result(originalMessage().replaceEmoteIds(player()))
    }

    // Replace with result not original message to avoid borking other chat formatting
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun AsyncChatDecorateEvent.onPlayerChat() {
        result(result().replaceEmoteIds(player()))
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerInteractEvent.onPlayerOpenBook() {
        if (!rightClicked) return
        val bookMeta = item?.itemMeta as? BookMeta ?: return
        val author = if (bookMeta.hasAuthor()) Bukkit.getPlayer(bookMeta.author().toString()) else null
        player.openBook(bookMeta.pages(bookMeta.pages().map { it.replaceEmoteIds(author, true) }))
        isCancelled = true
    }
}

//TODO Tags like rainbow and gradient, which split the text into multiple children, will break replacement below
fun Component.replaceEmoteIds(player: Player? = null, insert: Boolean = true): Component {
    var msg = this

    if (emojyConfig.emotes.none { ":${it.id}" in msg.serialize() } &&
        emojyConfig.gifs.none { ":${it.id}" in msg.serialize() }) return msg

    emojyConfig.emotes.forEach { emote ->
        if (emote.checkPermission(player)) {
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .matchLiteral(":${emote.id}:")
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
                    .replacement(gif.getFormattedUnicode(insert = insert))
                    .build()
            )
        }
    }
    return msg
}
