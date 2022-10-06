package com.mineinabyss.emojy

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.reflect.FieldAccessException
import com.comphenix.protocol.wrappers.WrappedChatComponent
import com.mineinabyss.idofront.messaging.miniMsg
import com.mineinabyss.idofront.messaging.serialize
import net.kyori.adventure.text.Component

class EmojyPackets : PacketAdapter(
    emojy,
    PacketType.Play.Server.SET_TITLE_TEXT,
    PacketType.Play.Server.SET_SUBTITLE_TEXT,
) {

    override fun onPacketSending(event: PacketEvent) {
        val chat = event.packet.chatComponents
        try {
            val title =
                (if (chat.read(0) == null) (event.packet.modifier.read(1) as Component)
                else chat.read(0).json.readJson()).replaceEmoteIds().removeUnwanted()

            chat.write(0, WrappedChatComponent.fromText(title.serialize()))
//            when (event.packetType) {
//                PacketType.Play.Server.SET_TITLE_TEXT -> event.player.sendTitlePart(TitlePart.TITLE, title)
//                PacketType.Play.Server.SET_SUBTITLE_TEXT -> event.player.sendTitlePart(TitlePart.SUBTITLE, title)
//                else -> {
//                }
//            }
        } catch (e: Exception) {
            when (e) {
                is NullPointerException, is FieldAccessException ->
                    event.player.sendMessage("fail")
                else -> {}
            }
        } catch (_: StackOverflowError) {}
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
