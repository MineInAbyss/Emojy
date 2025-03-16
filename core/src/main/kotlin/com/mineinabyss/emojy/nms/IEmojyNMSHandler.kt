package com.mineinabyss.emojy.nms

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player

interface IEmojyNMSHandler {

    fun inject(player: Player)
    fun uninject(player: Player)

    companion object {
        val legacyHandler = LegacyComponentSerializer.legacy('§')
        val EMOJY_CHANNEL_HANDLER = "emojy_channel_handler"
    }

    val supported get() = false
}
