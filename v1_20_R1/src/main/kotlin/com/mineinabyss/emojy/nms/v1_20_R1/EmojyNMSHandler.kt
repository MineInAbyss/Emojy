@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_20_R1

import com.google.gson.JsonParser
import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.legacy
import com.mineinabyss.emojy.nms.EmojyNMSHandlers
import com.mineinabyss.emojy.nms.EmojyNMSHandlers.formatString
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.transform
import com.mineinabyss.idofront.messaging.broadcast
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.network.*
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket
import net.minecraft.network.protocol.game.ServerboundChatPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerConnectionListener
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer
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
        val connections = MinecraftServer.getServer().connection?.connections ?: emptyList()
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
            val handler = if (prevHandler is PacketEncoder) PacketEncoder(PacketFlow.CLIENTBOUND) else prevHandler
            handler?.let { this.pipeline().replace("encoder", "encoder", handler) }
        }

        if (this in decoder.keys) {
            val prevHandler = decoder.remove(this)
            val handler = if (prevHandler is PacketDecoder) PacketDecoder(PacketFlow.SERVERBOUND) else prevHandler
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

    private class CustomDataSerializer(val player: Player?, bytebuf: ByteBuf) : FriendlyByteBuf(bytebuf) {
        override fun writeComponent(component: net.kyori.adventure.text.Component): FriendlyByteBuf {
            return super.writeComponent(component.transform(null, true, true))
        }

        override fun writeUtf(string: String, maxLength: Int): FriendlyByteBuf {
            runCatching {
                val element = JsonParser.parseString(string)
                if (element.isJsonObject) return super.writeUtf(element.asJsonObject.formatString(), maxLength)
            }

            return super.writeUtf(string, maxLength)
        }

        override fun writeNbt(compound: CompoundTag?): FriendlyByteBuf {
            return super.writeNbt(compound?.apply {
                transform(this, EmojyNMSHandlers.transformer())
            })
        }

        private fun transform(compound: CompoundTag, transformer: Function<String, String>) {
            for (key in compound.allKeys) when (val base = compound.get(key)) {
                is CompoundTag -> transform(base, transformer)
                is ListTag -> transform(base, transformer)
                is StringTag -> compound.put(key, StringTag.valueOf(transformer.apply(base.asString)))
            }
        }

        private fun transform(list: ListTag, transformer: Function<String, String>) {
            for (base in list) when (base) {
                is CompoundTag -> transform(base, transformer)
                is ListTag -> transform(base, transformer)
                is StringTag -> list.indexOf(base).let { index ->
                    list.add(index, StringTag.valueOf(transformer.apply(base.asString)))
                    list.removeAt(index + 1)
                }
            }
        }

        override fun readUtf(maxLength: Int): String {
            return super.readUtf(maxLength).let { string ->
                runCatching { string.miniMsg() }.recover { legacy.deserialize(string) }
                    .getOrNull()?.transform(player, true)?.serialize() ?: string
            }
        }

    }

    private class CustomPacketEncoder(val player: Player?) : MessageToByteEncoder<Packet<*>>() {
        private val protocolDirection = PacketFlow.CLIENTBOUND

        override fun encode(ctx: ChannelHandlerContext, msg: Packet<*>, out: ByteBuf) {
            val enumProt = ctx.channel()?.attr(Connection.ATTRIBUTE_PROTOCOL)?.get() ?: throw RuntimeException("ConnectionProtocol unknown: $out")
            val int = msg.let { enumProt.getPacketId(this.protocolDirection, it) }
            val packetDataSerializer: FriendlyByteBuf = CustomDataSerializer(player, out)
            packetDataSerializer.writeVarInt(int)

            runCatching {
                val int2 = packetDataSerializer.writerIndex()
                msg.write(packetDataSerializer)
                val int3 = packetDataSerializer.writerIndex() - int2
                if (int3 > 8388608) {
                    throw IllegalArgumentException("Packet too big (is $int3, should be less than 8388608): $msg")
                }
            }.onFailure {
                if (msg.isSkippable) throw SkipPacketException(it)
                throw it
            }
        }
    }

    private class CustomPacketDecoder(val player: Player?) : ByteToMessageDecoder() {

        override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf, out: MutableList<Any>) {
            val buffferCopy = buffer.copy()
            if (buffer.readableBytes() == 0) return

            val customDataSerializer: FriendlyByteBuf = CustomDataSerializer(player, buffer)
            val packetID = customDataSerializer.readVarInt()
            val protocol = ctx.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get()
            val packet = protocol.createPacket(PacketFlow.SERVERBOUND, packetID, customDataSerializer) ?: throw IOException("Bad packet id $packetID")

            out += when {
                customDataSerializer.readableBytes() > 0 -> throw IOException("Packet $packetID ($packet) was larger than I expected, found ${customDataSerializer.readableBytes()} bytes extra whilst reading packet $packetID")
                packet is ServerboundChatPacket || packet is ClientboundPlayerChatPacket -> {
                    broadcast((player?.name ?: "null") + ": " + packet.javaClass.name)
                    val baseSerializer = FriendlyByteBuf(buffferCopy)
                    val basePacketId = baseSerializer.readVarInt()
                    protocol.createPacket(PacketFlow.SERVERBOUND, basePacketId, baseSerializer) ?: throw IOException("Bad packet id $basePacketId")
                }
                else -> packet
            }
        }
    }

    override val supported get() = true
}
