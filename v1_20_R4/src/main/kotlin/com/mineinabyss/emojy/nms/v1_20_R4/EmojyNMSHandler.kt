@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_20_R4

import com.mineinabyss.emojy.EmojyPlugin
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.idofront.plugin.listeners
import net.minecraft.network.Connection
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

class EmojyNMSHandler(emojy: EmojyPlugin) : IEmojyNMSHandler {

    override fun inject(player: Player) {
        val channel = (player as CraftPlayer).handle.connection.connection.channel
        val pipeline = channel.pipeline()

        val emojyChannelHandler = EmojyChannelHandler(player)
        pipeline.toMap().keys.forEach {
            if (pipeline.get(it) !is Connection) return@forEach
            pipeline.addBefore(it, IEmojyNMSHandler.EMOJY_CHANNEL_HANDLER, emojyChannelHandler)
        }
    }

    override fun uninject(player: Player) {
        val channel = (player as CraftPlayer).handle.connection.connection.channel
        val pipeline = channel.pipeline()

        channel.eventLoop().submit {
            pipeline.remove(IEmojyNMSHandler.EMOJY_CHANNEL_HANDLER)
        }
    }

    init {
        emojy.listeners(EmojyListener(this))
    }

    override val supported get() = true
}
