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
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerJoinEvent

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerEditBookEvent.onPlayerWriteBook() {
        newBookMeta = newBookMeta.apply {
            pages(pages().map { page -> page.replaceEmoteIds(player) })
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun SignChangeEvent.onSignChange() = lines().forEachIndexed { i, l -> line(i, l.replaceEmoteIds(player)) }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerJoinEvent.onJoin() {
        player.sendPlayerListHeaderAndFooter(
            player.playerListHeader()?.replaceEmoteIds() ?: "".miniMsg(),
            player.playerListFooter()?.replaceEmoteIds() ?: "".miniMsg()
        )
    }

    // TODO Please change this when https://github.com/PaperMC/Paper/pull/7979 is merged
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun InventoryOpenEvent.onInvOpen() {
    }
}

fun Component.replaceEmoteIds(player: Player? = null): Component {
    var msg = this
    emojyConfig.emotes.entries.forEach { (id, emote) ->
        if (emote.checkPermission(player)) {
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .match(":$id:")
                    .replacement(emote.getFormattedUnicode())
                    .build()
            )
        }
    }
    return msg
}
