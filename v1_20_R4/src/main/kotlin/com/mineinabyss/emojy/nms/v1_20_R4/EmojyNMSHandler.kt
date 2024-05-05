@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_20_R4

import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.escapeEmoteIDs
import com.mineinabyss.emojy.legacy
import com.mineinabyss.emojy.nms.EmojyNMSHandlers
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import net.minecraft.core.RegistryAccess
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.*
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.GameProtocols
import net.minecraft.network.protocol.game.ServerboundChatPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerConnectionListener
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.io.IOException
import java.util.*
import java.util.function.Function

class EmojyNMSHandler : IEmojyNMSHandler {
    private val encoder = Collections.synchronizedMap(WeakHashMap<Channel, ChannelHandler>())
    private val decoder = Collections.synchronizedMap(WeakHashMap<Channel, ChannelHandler>())

    @Suppress("unused", "UNCHECKED_CAST", "FunctionName")
    fun EmojyNMSHandler() {
        val connections = MinecraftServer.getServer().connection.connections ?: emptyList()
        // Have to set it accessible because unlike connections it is private
        val channelFutures = ServerConnectionListener::class.java.getDeclaredField("f").apply { this.isAccessible = true; }.get(MinecraftServer.getServer().connection) as List<ChannelFuture>


        // Handle connected channels
        val endInitProtocol: ChannelInitializer<Channel> = object : ChannelInitializer<Channel>() {
            override fun initChannel(channel: Channel) {
                runCatching {
                    synchronized(connections) {
                        // Stop injecting channels
                        channel.eventLoop().submit { channel.inject() }
                    }
                }.onFailure { it.printStackTrace() }
            }
        }

        // Handle channels that are connecting
        val beginInitProtocol = object : ChannelInitializer<Channel>() {
            override fun initChannel(channel: Channel) {
                var handler: ChannelHandler? = null

                channel.pipeline().forEach {
                    if (it.value.javaClass.name == "com.viaversion.viaversion.bukkit.handlers.BukkitChannelInitializer") {
                        handler = it.value as ChannelHandler
                    }
                }
                handler?.let {
                    val initChannel = ChannelInitializer::class.java.getDeclaredMethod("initChannel", Channel::class.java).apply { isAccessible = true }
                    val original = it.javaClass.getDeclaredField("original").apply { this.isAccessible = true }
                    val initializer = original.get(it) as ChannelInitializer<*>
                    val miniInit = object : ChannelInitializer<Channel>() {
                        override fun initChannel(channel: Channel) {
                            initChannel.invoke(initializer, channel)
                            channel.eventLoop().submit { channel.inject() }
                        }
                    }
                    original.set(handler, miniInit)
                } ?: channel.pipeline().addLast(endInitProtocol)
            }
        }

        val serverChannelHandler = object : ChannelInboundHandlerAdapter() {
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                (msg as Channel).pipeline().addFirst(beginInitProtocol)
                ctx.fireChannelRead(msg)
            }
        }

        runCatching {
            bind(channelFutures, serverChannelHandler)
        }.onFailure {
            object : BukkitRunnable() {
                override fun run() {
                    bind(channelFutures, serverChannelHandler)
                }
            }.runTask(emojy.plugin)
        }
    }

    private fun bind(channelFutures: List<ChannelFuture>, serverChannelHandler: ChannelInboundHandlerAdapter) {
        channelFutures.forEach { future ->
            future.channel().pipeline().addFirst(serverChannelHandler)
        }

        Bukkit.getOnlinePlayers().forEach(::inject)
    }

    private fun Channel.inject(player: Player? = null) {
        if (this !in encoder.keys && (this.pipeline().get("encoder") as ChannelHandler) !is CustomPacketEncoder)
            encoder[this] = this.pipeline().replace("encoder", "encoder", CustomPacketEncoder(player))

        if (this !in decoder.keys && (this.pipeline().get("decoder") as ChannelHandler) !is CustomPacketDecoder)
            decoder[this] = this.pipeline().replace("decoder", "decoder", CustomPacketDecoder(player))

    }

    private fun Channel.uninject() {
        if (this in encoder.keys) {
            val prevHandler = encoder.remove(this)
            val handler = if (prevHandler is PacketEncoder<*>) PacketEncoder(GameProtocols.CLIENTBOUND.bind(RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY))) else prevHandler
            handler?.let { this.pipeline().replace("encoder", "encoder", handler) }
        }

        if (this in decoder.keys) {
            val prevHandler = decoder.remove(this)
            val handler = if (prevHandler is PacketDecoder<*>) PacketDecoder(GameProtocols.SERVERBOUND.bind(RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY))) else prevHandler
            handler?.let { this.pipeline().replace("decoder", "decoder", handler) }
        }
    }

    override fun inject(player: Player) {
        val channel = (player as? CraftPlayer)?.handle?.connection?.connection?.channel ?: return
        channel.eventLoop().submit { channel.inject(player) }
    }

    override fun uninject(player: Player) {
        (player as? CraftPlayer)?.handle?.connection?.connection?.channel?.uninject()
    }

    private class CustomDataSerializer(val player: Player?, bytebuf: ByteBuf) : RegistryFriendlyByteBuf(bytebuf, RegistryAccess.EMPTY) {

        override fun writeUtf(string: String, maxLength: Int): FriendlyByteBuf {
            return EmojyNMSHandlers.writeTransformer(player, true, true).invoke(string).let { super.writeUtf(it, maxLength) }
        }

        override fun readUtf(maxLength: Int): String {
            return super.readUtf(maxLength).let { string ->
                runCatching { string.miniMsg() }.recover { legacy.deserialize(string) }
                    .getOrNull()?.escapeEmoteIDs(player)?.serialize() ?: string
            }
        }

        override fun writeNbt(compound: Tag?): FriendlyByteBuf {
            return super.writeNbt(compound?.apply {
                when (this) {
                    is CompoundTag -> transform(this, EmojyNMSHandlers.writeTransformer(player, false, true))
                    is StringTag -> transform(this, EmojyNMSHandlers.writeTransformer(player, false,true))
                }
            })
        }

        override fun readNbt(): CompoundTag? {
            return super.readNbt()?.apply {
                transform(this, EmojyNMSHandlers.readTransformer(player))
            }
        }

        private fun transform(compound: CompoundTag, transformer: Function<String, String>) {
            for (key in compound.allKeys) when (val base = compound.get(key)) {
                is CompoundTag -> transform(base, transformer)
                is ListTag -> transform(base, transformer)
                is StringTag -> compound.put(key, StringTag.valueOf(transformer.apply(base.asString)))
            }
        }

        private fun transform(list: ListTag, transformer: Function<String, String>) {
            val listCopy = list.toList()
            for (base in listCopy) when (base) {
                is CompoundTag -> transform(base, transformer)
                is ListTag -> transform(base, transformer)
                is StringTag -> list.indexOf(base).let { index ->
                    list[index] = StringTag.valueOf(transformer.apply(base.asString))
                }
            }
        }

        private fun transform(string: StringTag, transformer: Function<String, String>) {
            transformer.apply(string.asString)
        }

    }

    private class CustomPacketEncoder(val player: Player? = null) : MessageToByteEncoder<Packet<*>>() {
        private val protocolDirection = PacketFlow.CLIENTBOUND

        override fun encode(ctx: ChannelHandlerContext, msg: Packet<*>, out: ByteBuf) {
            GameProtocols.CLIENTBOUND.bind { CustomDataSerializer(player, it) }.codec().encode(out, msg as Packet<in ClientGamePacketListener>)
        }
    }

    private class CustomPacketDecoder(val player: Player? = null) : ByteToMessageDecoder() {

        override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf, out: MutableList<Any>) {
            val bufferCopy = buffer.copy()
            if (buffer.readableBytes() == 0) return

            val customDataSerializer = CustomDataSerializer(player, buffer)
            val codec = GameProtocols.SERVERBOUND.bind { CustomDataSerializer(player, it) }.codec()
            var packet = codec.decode(buffer)

            out += when {
                customDataSerializer.readableBytes() > 0 -> throw IOException("Packet ($packet) was larger than I expected, found ${customDataSerializer.readableBytes()} bytes extra whilst reading packet")
                packet is ServerboundChatPacket -> packet = codec.decode(bufferCopy)
                else -> packet
            }
            ProtocolSwapHandler.handleInboundTerminalPacket(ctx, packet)
        }
    }

    override val supported get() = true
}
