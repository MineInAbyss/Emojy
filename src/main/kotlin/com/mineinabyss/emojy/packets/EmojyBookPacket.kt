package com.mineinabyss.emojy.packets

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.packets.PacketHelpers.removeUnwanted
import com.mineinabyss.emojy.replaceEmoteIds
import net.kyori.adventure.inventory.Book

class EmojyBookPacket : PacketAdapter(emojy, PacketType.Play.Server.OPEN_BOOK) {

    override fun onPacketSending (event: PacketEvent) {
        val player = event.player
        val item = player.inventory.run { itemInMainHand.itemMeta as? Book ?: itemInOffHand.itemMeta as? Book } ?: return
        val book = item.pages(item.pages().map { it.replaceEmoteIds(player, insert = false).removeUnwanted() })
        //event.isCancelled = true
        player.openBook(book)
    }
}
