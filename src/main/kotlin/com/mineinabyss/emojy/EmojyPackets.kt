package com.mineinabyss.emojy

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.mineinabyss.idofront.messaging.miniMsg
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
//        val packet = event.packet
//        val chat = packet.chatComponents
//        val p = event.player
//        val title = chat.read(0).json.readJson()
//        event.isCancelled = true
//
//        when (packet.type) {
//            PacketType.Play.Server.SET_TITLE_TEXT ->
//                p.sendTitlePart(TitlePart.TITLE, title)
//            PacketType.Play.Server.SET_SUBTITLE_TEXT ->
//                p.sendTitlePart(TitlePart.SUBTITLE, title)
//            PacketType.Play.Server.SET_ACTION_BAR_TEXT ->
//                p.sendActionBar(title.replaceEmoteIds())
//            PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER ->
//                p.sendPlayerListHeaderAndFooter(title, chat.read(1).json.readJson())
//        }
    }

    // Ugly but works :)
    private fun String.readJson(): Component =
        this.substringBeforeLast("\"}").substringAfter("{\"text\":\"").miniMsg().replaceEmoteIds()
}
