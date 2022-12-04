package com.mineinabyss.emojy

import com.google.gson.JsonParser
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.papermc.paper.network.ChannelInitializeListener
import net.minecraft.network.Connection
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerConnectionListener
import java.util.*

class EmojyNMSHandler {
    private val encoder = Collections.synchronizedMap(WeakHashMap<Channel, ChannelHandler>())
    private val decoder = Collections.synchronizedMap(WeakHashMap<Channel, ChannelHandler>())
    private val transformer: ComponentTransformer? = null
    private val parser = JsonParser()

    fun d() {
        val networkManagers : List<Connection> = MinecraftServer.getServer().connection?.connections ?: emptyList()
        var channelFutures: List<ChannelFuture> = emptyList()

        try {
            val channelFutureField = ServerConnectionListener::class.java.getDeclaredField("channelFuture").apply {
                isAccessible = true
            }

            //TODO Is this even needed?
            //networkManagers = networkManagerField.get(MinecraftServer.getServer().connection) as List<Connection>
            channelFutures = channelFutureField.get(MinecraftServer.getServer().connection) as List<ChannelFuture>
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val managers = networkManagers
        val futures = channelFutures

        ChannelInitializeListener {
            it.eventLoop().submit()
        }
    }

    private fun Channel.inject() {
        if (!encode)
    }
}
