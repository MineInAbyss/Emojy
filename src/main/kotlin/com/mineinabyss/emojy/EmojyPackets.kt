package com.mineinabyss.emojy

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.mineinabyss.idofront.messaging.logVal

class EmojyPackets : PacketAdapter(
    emojy,
    PacketType.Play.Server.SET_TITLE_TEXT,
    PacketType.Play.Server.SET_SUBTITLE_TEXT,
    //PacketType.Play.Server.SET_ACTION_BAR_TEXT
) {

    override fun onPacketSending(event: PacketEvent) {
        val title = event.packet.chatComponents.read(0).json.logVal()
        event.isCancelled = true
//        when (event.packet.type) {
//            PacketType.Play.Server.SET_TITLE_TEXT -> event.player.sendTitlePart(
//                TitlePart.TITLE,
//                title.miniMsg().replaceEmoteIds()
//            )
//
//            PacketType.Play.Server.SET_SUBTITLE_TEXT -> event.player.sendTitlePart(
//                TitlePart.SUBTITLE,
//                title.miniMsg().replaceEmoteIds()
//            )
//        }
    }
}
