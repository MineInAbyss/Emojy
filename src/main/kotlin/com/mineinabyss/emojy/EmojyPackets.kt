package com.mineinabyss.emojy

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent

class EmojyPackets : PacketAdapter(
    emojy,
    PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER,
    PacketType.Play.Server.SET_TITLE_TEXT,
    PacketType.Play.Server.SET_SUBTITLE_TEXT,
    PacketType.Play.Server.SET_ACTION_BAR_TEXT
) {

    override fun onPacketSending(event: PacketEvent) {
//        val packet = event.packet
//        val chat = packet.chatComponents
//        val p = event.player
//        val title = chat.read(0).json.substringBeforeLast("\"}").substringAfter("{\"text\":\"").miniMsg()
//        event.isCancelled = true
//
//        when (packet.type) {
//            PacketType.Play.Server.SET_TITLE_TEXT ->
//                p.sendTitlePart(TitlePart.TITLE, title.replaceEmoteIds())
//            PacketType.Play.Server.SET_SUBTITLE_TEXT ->
//                p.sendTitlePart(TitlePart.SUBTITLE, title.replaceEmoteIds())
//            PacketType.Play.Server.SET_ACTION_BAR_TEXT ->
//                p.sendActionBar(title.replaceEmoteIds())
//            PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER -> {
//                val header = chat.read(0).json.substringBeforeLast("\"}").substringAfter("{\"text\":\"").miniMsg().replaceEmoteIds()
//                val footer = chat.read(1).json.substringBeforeLast("\"}").substringAfter("{\"text\":\"").miniMsg().replaceEmoteIds()
//                p.sendPlayerListHeaderAndFooter(header, footer)
//            }
//        }
    }
}
