package com.mineinabyss.emojy.packets

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.reflect.FieldAccessException
import com.comphenix.protocol.wrappers.WrappedChatComponent
import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.packets.PacketHelpers.readJson
import com.mineinabyss.emojy.replaceEmoteIds

class EmojyInventoryPacket : PacketAdapter(emojy, PacketType.Play.Server.OPEN_WINDOW) {

    override fun onPacketReceiving(event: PacketEvent) {
        val chat = event.packet.chatComponents
        try {
            val title = chat.read(0).json.readJson().replaceEmoteIds(insert = false)
            chat.write(0, WrappedChatComponent.fromJson(gson.serialize(title)))
        } catch (e: Exception) {
            when (e) {
                is NullPointerException, is FieldAccessException -> {}
            }
        }
    }
}
