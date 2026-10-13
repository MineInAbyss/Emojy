package com.mineinabyss.emojy.velocity

import com.mineinabyss.emojy.transformEmotes
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.proxy.protocol.MinecraftPacket
import com.velocitypowered.proxy.protocol.packet.BossBarPacket
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooterPacket
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder
import com.velocitypowered.proxy.protocol.packet.title.LegacyTitlePacket
import com.velocitypowered.proxy.protocol.packet.title.TitleActionbarPacket
import com.velocitypowered.proxy.protocol.packet.title.TitleSubtitlePacket
import com.velocitypowered.proxy.protocol.packet.title.TitleTextPacket
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import net.kyori.adventure.text.Component
import org.slf4j.Logger
import java.util.*

/**
 * Chat packets are deliberately left alone.
 * Backends unescape emotes a player was not allowed to use back into plain `:id:` text,
 * which a second pass here would turn into the emote anyway
 */
class EmojyProxyChannelHandler(
    private val player: Player,
    private val velocitab: Boolean,
    private val logger: Logger,
) : ChannelDuplexHandler() {

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        val packet = (msg as? MinecraftPacket)?.let { packet ->
            runCatching { transform(packet) }
                .onFailure { logger.debug("Failed to transform ${packet.javaClass.simpleName}", it) }
                .getOrNull()
        } ?: msg
        super.write(ctx, packet, promise)
    }

    private fun transform(packet: MinecraftPacket): MinecraftPacket = when (packet) {
        is HeaderAndFooterPacket -> HeaderAndFooterPacket(packet.header.transform(), packet.footer.transform())
        is UpsertPlayerInfoPacket -> packet.apply { entries.forEach { it.displayName = it.displayName?.transform() } }
        is BossBarPacket -> packet.apply { name = name?.transform() }
        is TitleTextPacket -> packet.apply { component = component?.transform() }
        is TitleSubtitlePacket -> packet.apply { component = component?.transform() }
        is TitleActionbarPacket -> packet.apply { component = component?.transform() }
        is LegacyTitlePacket -> packet.apply { component = component?.transform() }
        else -> if (velocitab) VelocitabTeams.transform(packet) { it.transform() } else packet
    }

    private fun Component.transform() = transformEmotes(player.effectiveLocale ?: Locale.US)

    private fun ComponentHolder.transform() = ComponentHolder(player.protocolVersion, component.transform())

    companion object {
        const val NAME = "emojy_proxy_handler"
    }
}
