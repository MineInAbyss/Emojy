package com.mineinabyss.emojy.nms.v1_20_R4

import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.escapeEmoteIDs
import com.mineinabyss.emojy.transformEmoteIDs
import com.mineinabyss.idofront.items.editItemMeta
import com.mineinabyss.idofront.textcomponents.miniMsg
import io.papermc.paper.event.player.AsyncChatDecorateEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerJoinEvent

@Suppress("UnstableApiUsage")
class EmojyListener : Listener {

    @EventHandler
    fun PlayerJoinEvent.onJoin() {
        emojy.handler.addLocaleCodec(player.locale())
    }

    // Replace with result not original message to avoid borking other chat formatting
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun AsyncChatDecorateEvent.onPlayerChat() {
        result(result().escapeEmoteIDs(player()))
    }

    @EventHandler
    fun PrepareAnvilEvent.onAnvil() {
        if (result?.itemMeta?.hasDisplayName() != true) return
        val displayName = (inventory.renameText?.miniMsg() ?: inventory.firstItem?.itemMeta?.displayName())?.transformEmoteIDs(null, false, true)
        result = result?.editItemMeta {
            displayName(displayName)
        }
    }
}

