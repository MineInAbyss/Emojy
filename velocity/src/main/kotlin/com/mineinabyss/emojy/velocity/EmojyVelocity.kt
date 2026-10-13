package com.mineinabyss.emojy.velocity

import com.google.inject.Inject
import com.mineinabyss.emojy.EmotePayload
import com.mineinabyss.emojy.EmoteRegistry
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.proxy.connection.client.ConnectedPlayer
import com.velocitypowered.proxy.network.Connections
import org.slf4j.Logger
import java.nio.file.Path
import kotlin.jvm.optionals.getOrNull

class EmojyVelocity @Inject constructor(
    private val proxy: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path,
) {
    private val emotesChannel = MinecraftChannelIdentifier.from(EmotePayload.CHANNEL)
    private val handshakeChannel = MinecraftChannelIdentifier.from(EmotePayload.HANDSHAKE_CHANNEL)
    private val registry = ProxyEmoteRegistry(dataDirectory.resolve("cache"), logger)
    private val velocitab by lazy { proxy.pluginManager.isLoaded("velocitab") }

    @Subscribe
    fun ProxyInitializeEvent.onProxyInitialization() {
        registry.loadCache()
        EmoteRegistry.instance = registry
        proxy.channelRegistrar.register(emotesChannel, handshakeChannel)
    }

    @Subscribe
    fun LoginEvent.onLogin() {
        val player = player as? ConnectedPlayer ?: return
        player.connection.channel.pipeline()
            .addBefore(Connections.HANDLER, EmojyProxyChannelHandler.NAME, EmojyProxyChannelHandler(player, velocitab, logger))
    }

    @Subscribe
    fun ServerPostConnectEvent.onServerPostConnect() {
        val server = player.currentServer.getOrNull() ?: return
        server.sendPluginMessage(handshakeChannel, registry.hash(server.serverInfo.name).encodeToByteArray())
    }

    @Subscribe
    fun PluginMessageEvent.onPluginMessage() {
        when (identifier.id) {
            EmotePayload.HANDSHAKE_CHANNEL -> result = PluginMessageEvent.ForwardResult.handled()
            EmotePayload.CHANNEL -> {
                result = PluginMessageEvent.ForwardResult.handled()
                val serverName = (source as? ServerConnection)?.serverInfo?.name ?: return
                runCatching {
                    registry.accept(serverName, data)
                }.onFailure {
                    logger.warn("Failed to read emotes from ${serverName}: ${it.message}")
                }
            }
        }
    }
}
