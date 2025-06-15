package com.mineinabyss.emojy.nms.v1_21_R5

import com.jeff_media.morepersistentdatatypes.DataType
import com.mineinabyss.emojy.ORIGINAL_ITEM_RENAME_TEXT
import com.mineinabyss.emojy.escapeEmoteIDs
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.transformEmotes
import com.mineinabyss.emojy.unescapeEmoteIds
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.event.player.AsyncChatCommandDecorateEvent
import io.papermc.paper.event.player.AsyncChatDecorateEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

@Suppress("UnstableApiUsage")
class EmojyListener(val handler: IEmojyNMSHandler) : Listener {

    @EventHandler
    fun PlayerJoinEvent.onJoin() {
        handler.inject(player)
    }

    @EventHandler
    fun PlayerQuitEvent.onQuit() {
        handler.uninject(player)
    }

    // Replace with result not original message to avoid borking other chat formatting
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun AsyncChatDecorateEvent.onPlayerChat() {
        result(result().escapeEmoteIDs(player()))
    }

    @EventHandler
    fun AsyncChatCommandDecorateEvent.onPlayerCommandChat() {
        result(result().escapeEmoteIDs(player()))
    }

    @EventHandler
    fun PrepareAnvilEvent.onAnvil() {
        result?.editPersistentDataContainer {
            if (view.renameText == null || result?.hasData(DataComponentTypes.CUSTOM_NAME) != true)
                it.remove(ORIGINAL_ITEM_RENAME_TEXT)
            else it.set(ORIGINAL_ITEM_RENAME_TEXT, DataType.STRING, view.renameText!!)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerEditBookEvent.onBookEdit() {
        if (isSigning) newBookMeta = newBookMeta.apply {
            pages(pages().map {
                it.escapeEmoteIDs(player).transformEmotes().unescapeEmoteIds()
            })
        }
    }
}

