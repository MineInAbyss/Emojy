package com.mineinabyss.emojy

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.reflect.FieldAccessException
import com.comphenix.protocol.wrappers.WrappedChatComponent
import com.mineinabyss.idofront.messaging.broadcastVal
import com.mineinabyss.idofront.messaging.miniMsg
import com.mineinabyss.idofront.messaging.serialize
import net.kyori.adventure.text.Component

class EmojyPackets : PacketAdapter(
    emojy,
    //PacketType.Play.Server.OPEN_WINDOW,
    PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER,
    PacketType.Play.Server.SET_TITLE_TEXT,
    PacketType.Play.Server.SET_SUBTITLE_TEXT,
    PacketType.Play.Server.SET_ACTION_BAR_TEXT
) {

    override fun onPacketSending(event: PacketEvent) {
        val chat = event.packet.chatComponents
        try {
            val title =
                (if (chat.read(0) == null) (event.packet.modifier.read(1) as Component)
                else chat.read(0).toString().miniMsg()).replaceEmoteIds().removeUnwanted()
            chat.write(0, WrappedChatComponent.fromText(title.serialize().broadcastVal()))
        } catch (e: Exception) {
            when (e) {
                is NullPointerException, is FieldAccessException -> {
                    event.player.sendMessage("fail")
                }

                else -> e.printStackTrace()
            }
        }

//        try {
//            (event.packet.modifier.read(1) == null).broadcastVal()
//            val subtitle = if (chat.read(1) == null) {
//                (event.packet.modifier.read(1) as Component).replaceEmoteIds().removeUnwanted()
//            } else chat.read(1).json.readJson()
//            chat.write(1, WrappedChatComponent.fromText(subtitle.serialize()))
//        } catch (e: Exception) {
//            when (e) {
//                is FieldAccessException, is NullPointerException -> {
//                    event.player.sendMessage("ccc")
//                }
//
//                else -> e.printStackTrace()
//            }
//        }
    }

    // Ugly but works :)
    private fun String.readJson(): Component =
        this.substringBeforeLast("\"}").substringAfter("{\"text\":\"").miniMsg().replaceEmoteIds().removeUnwanted()

    private fun Component.readJson(): Component =
        this.serialize().substringBeforeLast("\"}").substringAfter("{\"text\":\"").miniMsg().replaceEmoteIds()
            .removeUnwanted()

    private fun String.toJson(): String = "{\"text\":\"${this}\"}"
    private fun Component.removeUnwanted(): Component {
        return this.insertion(null).clickEvent(null).hoverEvent(null)
    }
}
