package com.mineinabyss.emojy

import com.mineinabyss.emojy.nms.EmojyNMSHandlers
import io.papermc.paper.event.player.AsyncChatDecorateEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

@Suppress("UnstableApiUsage")
class EmojyListener : Listener {

    @EventHandler
    fun PlayerJoinEvent.injectPlayer() {
        EmojyNMSHandlers.getHandler()?.inject(player)
    }

    // Replace with result not original message to avoid borking other chat formatting
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun AsyncChatDecorateEvent.onPlayerChat() {
        //result(result().replaceEmoteIds(player()))
    }
}

