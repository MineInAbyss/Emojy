package com.mineinabyss.emojy

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.reflect.FieldAccessException
import com.mineinabyss.idofront.messaging.broadcastVal
import com.mineinabyss.idofront.messaging.miniMsg
import net.kyori.adventure.text.Component

class EmojyPackets : PacketAdapter(
    emojy,
    PacketType.Play.Server.SET_TITLE_TEXT,
    PacketType.Play.Server.SET_SUBTITLE_TEXT,
    PacketType.Play.Server.SET_ACTION_BAR_TEXT,
    PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER
) {

    override fun onPacketSending(event: PacketEvent) {
        val chat = event.packet.chatComponents
        if (event.packetType != PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER) {
            try {
                val title =
                    (if (chat.read(0) == null) (event.packet.modifier.read(1) as Component)
                    else chat.read(0).json.readJson()).replaceEmoteIds().removeUnwanted()

                event.packet.modifier.write(1, title)
            } catch (e: Exception) {
                when (e) {
                    is NullPointerException, is FieldAccessException ->
                        event.player.sendMessage("fail")
                    else -> {event.player.sendMessage("fail2")}
                }
            }
        } else {
            try {
                val header =
                    (if (chat.read(0) == null) (event.packet.modifier.read(2) as Component).broadcastVal()
                    else chat.read(1).json.readJson()).replaceEmoteIds().removeUnwanted()

                event.packet.modifier.write(2, header)
            } catch (e: Exception) {
                when (e) {
                    is NullPointerException, is FieldAccessException ->
                        event.player.sendMessage("fail4")
                    else -> {event.player.sendMessage("fail2")}
                }
            }

            try {
                val footer =
                    (if (chat.read(1) == null) (event.packet.modifier.read(3) as Component).broadcastVal()
                    else chat.read(1).json.readJson()).replaceEmoteIds().removeUnwanted()

                event.packet.modifier.write(3, footer)
            } catch (e: Exception) {
                when (e) {
                    is NullPointerException, is FieldAccessException ->
                        event.player.sendMessage("fail3")
                    else -> {event.player.sendMessage("fail2")}
                }
            }
        }
    }

    // Ugly but works :)
    private fun String.readJson(): Component =
        this.substringBeforeLast("\"}").substringAfter("{\"text\":\"").miniMsg()

    private fun Component.removeUnwanted(): Component {
        return this.insertion(null).clickEvent(null).hoverEvent(null)
    }
}
