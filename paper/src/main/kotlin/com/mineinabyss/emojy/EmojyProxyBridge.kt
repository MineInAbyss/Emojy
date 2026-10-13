package com.mineinabyss.emojy

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.messaging.PluginMessageListener

class EmojyProxyBridge(private val plugin: Plugin, registry: EmoteRegistry) : PluginMessageListener {
    private val payload by lazy { EmotePayload.encode(registry) }
    private val hash by lazy { EmotePayload.hash(payload) }

    fun register(): Unit = with(plugin.server.messenger) {
        registerOutgoingPluginChannel(plugin, EmotePayload.CHANNEL)
        registerIncomingPluginChannel(plugin, EmotePayload.HANDSHAKE_CHANNEL, this@EmojyProxyBridge)
    }

    fun unregister(): Unit = with(plugin.server.messenger) {
        unregisterOutgoingPluginChannel(plugin, EmotePayload.CHANNEL)
        unregisterIncomingPluginChannel(plugin, EmotePayload.HANDSHAKE_CHANNEL, this@EmojyProxyBridge)
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != EmotePayload.HANDSHAKE_CHANNEL || message.decodeToString() == hash) return
        player.sendPluginMessage(plugin, EmotePayload.CHANNEL, payload)
    }
}
