package com.mineinabyss.emojy.packets

import com.comphenix.protocol.PacketType.Play.Server.*
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.reflect.FieldAccessException
import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.packets.PacketHelpers.readJson
import com.mineinabyss.emojy.replaceEmoteIds
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

val gson = GsonComponentSerializer.gson()
class EmojyTitlePacket : PacketAdapter(
    emojy,
    ListenerPriority.HIGHEST,
    SET_TITLE_TEXT,
    SET_SUBTITLE_TEXT,
    SET_ACTION_BAR_TEXT,
    PLAYER_LIST_HEADER_FOOTER,
) {

    override fun onPacketSending(event: PacketEvent) {
        val chat = event.packet.chatComponents

        when (event.packetType) {
            in setOf(SET_TITLE_TEXT, SET_SUBTITLE_TEXT, SET_ACTION_BAR_TEXT) -> {
                try {
                    val title =
                        (if (chat.read(0) == null) (event.packet.modifier.read(1) as Component)
                        else chat.read(0).json.readJson()).replaceEmoteIds(insert = false)
                    event.packet.modifier.write(1, title)
                } catch (e: Exception) {
                    when (e) {
                        is NullPointerException, is FieldAccessException -> {}
                    }
                }
            }
            PLAYER_LIST_HEADER_FOOTER -> {
                try {
                    val header =
                        (if (chat.read(0) == null) (event.packet.modifier.read(2) as Component)
                        else chat.read(1).json.readJson()).replaceEmoteIds(insert = false)
                    val footer =
                        (if (chat.read(1) == null) (event.packet.modifier.read(3) as Component)
                        else chat.read(1).json.readJson()).replaceEmoteIds(insert = false)

                    event.packet.modifier.write(2, header)
                    event.packet.modifier.write(3, footer)
                } catch (e: Exception) {
                    when (e) {
                        is NullPointerException, is FieldAccessException -> {}
                    }
                }
            }
        }
    }
}
