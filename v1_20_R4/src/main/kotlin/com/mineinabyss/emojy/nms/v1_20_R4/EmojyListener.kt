package com.mineinabyss.emojy.nms.v1_20_R4

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import com.jeff_media.morepersistentdatatypes.DataType
import com.mineinabyss.emojy.*
import com.mineinabyss.idofront.items.editItemMeta
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import io.papermc.paper.event.player.AsyncChatDecorateEvent
import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.event.player.PlayerOpenSignEvent
import kotlinx.coroutines.delay
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerEditBookEvent

@Suppress("UnstableApiUsage")
class EmojyListener : Listener {

    // Replace with result not original message to avoid borking other chat formatting
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun AsyncChatDecorateEvent.onPlayerChat() {
        result(result().escapeEmoteIDs(player()))
    }

    @EventHandler
    fun SignChangeEvent.onSign() {
        val state = (block.state as Sign)
        val type = DataType.asList(DataType.STRING)
        val sideLines = lines().map { it.serialize() }.toList()
        val frontLines = if (side == Side.FRONT) sideLines else state.persistentDataContainer.getOrDefault(ORIGINAL_SIGN_FRONT_LINES, type, mutableListOf("", "", "", ""))
        val backLines = if (side == Side.BACK) sideLines else state.persistentDataContainer.getOrDefault(ORIGINAL_SIGN_BACK_LINES, type, mutableListOf("", "", "", ""))

        state.persistentDataContainer.set(ORIGINAL_SIGN_FRONT_LINES, type, frontLines)
        state.persistentDataContainer.set(ORIGINAL_SIGN_BACK_LINES, type, backLines)
        state.update(true)

        lines().forEachIndexed { index, s ->
            line(index, s?.escapeEmoteIDs(player)?.transformEmotes())
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerOpenSignEvent.onSignEdit() {
        if (cause == PlayerOpenSignEvent.Cause.PLACE) return

        sign.persistentDataContainer.get(when (sign.getInteractableSideFor(player)) {
            Side.FRONT -> ORIGINAL_SIGN_FRONT_LINES
            Side.BACK -> ORIGINAL_SIGN_BACK_LINES
        }, DataType.asList(DataType.STRING))?.forEachIndexed { index, s ->
            sign.getSide(side).line(index, s.miniMsg())
        }
        sign.update(true)
        isCancelled = true
        emojy.plugin.launch {
            delay(2.ticks)
            (player as CraftPlayer).handle.level().getBlockEntity(BlockPos(sign.x, sign.y, sign.z), BlockEntityType.SIGN).ifPresent {
                it.setAllowedPlayerEditor(player.uniqueId)
                (player as CraftPlayer).handle.openTextEdit(it, side == Side.FRONT)
            }
        }
    }

    @EventHandler
    fun PrepareAnvilEvent.onAnvil() {
        result = result?.editItemMeta {
            if (inventory.renameText == null || result?.itemMeta?.hasDisplayName() != true) {
                persistentDataContainer.remove(ORIGINAL_ITEM_RENAME_TEXT) }
            else persistentDataContainer.set(ORIGINAL_ITEM_RENAME_TEXT, DataType.STRING, inventory.renameText!!)
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

