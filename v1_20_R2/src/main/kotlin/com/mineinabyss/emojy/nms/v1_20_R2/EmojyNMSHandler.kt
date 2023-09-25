@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_20_R2

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.replaceEmoteIds
import com.mineinabyss.idofront.textcomponents.miniMsg
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.*
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerConnectionListener
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.io.IOException
import java.util.*
import java.util.function.Function
import java.util.function.Supplier

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
                            channel.inject()
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

    private fun Channel.inject() {
        if (this !in encoder.keys && (this.pipeline().get("encoder") as ChannelHandler) !is CustomPacketEncoder)
            encoder[this] = this.pipeline().replace("encoder", "encoder", CustomPacketEncoder())

        if (this !in decoder.keys && (this.pipeline().get("decoder") as ChannelHandler) !is CustomPacketDecoder)
            decoder[this] = this.pipeline().replace("decoder", "decoder", CustomPacketDecoder())

    }

    private fun Channel.uninject() {
        if (this in encoder.keys) {
            val prevHandler = encoder.remove(this)
            val handler = if (prevHandler is PacketEncoder) PacketEncoder(Connection.ATTRIBUTE_CLIENTBOUND_PROTOCOL) else prevHandler
            handler?.let { this.pipeline().replace("encoder", "encoder", handler) }
        }

        if (this in decoder.keys) {
            val prevHandler = decoder.remove(this)
            val handler = if (prevHandler is PacketEncoder) PacketEncoder(Connection.ATTRIBUTE_SERVERBOUND_PROTOCOL) else prevHandler
            handler?.let { this.pipeline().replace("decoder", "decoder", handler) }
        }
    }

    override fun inject(player: Player) {
        val channel = (player as? CraftPlayer)?.handle?.connection?.connection?.channel ?: return
        channel.inject()
        channel.pipeline().forEach {
            when (val handler = it.value) {
                is CustomPacketEncoder -> handler.setPlayer(player)
                is CustomPacketDecoder -> handler.setPlayer(player)
            }
        }
    }

    override fun uninject(player: Player) {
        (player as? CraftPlayer)?.handle?.connection?.connection?.channel?.uninject()
    }

    private class CustomDataSerializer(val supplier: Supplier<Player>?, bytebuf: ByteBuf) :
        FriendlyByteBuf(bytebuf) {

        override fun writeUtf(string: String, maxLength: Int): FriendlyByteBuf {
            runCatching {
                val element = JsonParser.parseString(string)
                if (element.isJsonObject) return super.writeUtf(element.asJsonObject.formatString(), maxLength)
            }

            return super.writeUtf(string, maxLength)
        }

        override fun writeNbt(compound: Tag?): FriendlyByteBuf {
            return super.writeNbt(compound?.apply { this as CompoundTag
                transform(this, Function { string: String ->
                    return@Function runCatching {
                        val element = JsonParser.parseString(string)
                        if (element.isJsonObject) element.asJsonObject.formatString(false)
                        else string
                    }.getOrNull() ?: string
                })
            })
        }

        private fun JsonObject.formatString(insert: Boolean = true): String {
            val gson = GsonComponentSerializer.gson()
            return if (this.has("args") || this.has("text") || this.has("extra") || this.has("translate")) {
                gson.serialize(gson.deserialize(this.toString()).replaceEmoteIds(supplier?.get(), insert))
            } else this.toString()
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
            return super.readUtf(maxLength).apply {
                this.miniMsg().replaceEmoteIds(supplier?.get(), false)
            }
        }

    }

    private class CustomPacketEncoder : MessageToByteEncoder<Packet<*>>() {
        private val protocolDirection = PacketFlow.CLIENTBOUND
        private var player: Player? = null

        fun setPlayer(player: Player) {
            this.player = player
        }

        override fun encode(ctx: ChannelHandlerContext, msg: Packet<*>, out: ByteBuf) {
            val enumProt = ctx.channel()?.attr(Connection.ATTRIBUTE_CLIENTBOUND_PROTOCOL)?.get() ?: throw RuntimeException("ConnectionProtocol unknown: $out")
            val packetID = enumProt.protocol().codec(protocolDirection).packetId(msg)
            val packetDataSerializer: FriendlyByteBuf = CustomDataSerializer(Supplier<Player> { player }, out)
            packetDataSerializer.writeVarInt(packetID)

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

    private class CustomPacketDecoder : ByteToMessageDecoder() {
        private var player: Player? = null

        fun setPlayer(player: Player) {
            this.player = player
        }

        override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
            if (msg.readableBytes() == 0) return

            val dataSerializer = CustomDataSerializer(Supplier<Player> { player }, msg)
            val packetID = dataSerializer.readVarInt()
            val attribute = ctx.channel().attr(Connection.ATTRIBUTE_SERVERBOUND_PROTOCOL)
            val packet = attribute.get().createPacket(packetID, dataSerializer)
                ?: throw IOException("Bad packet id $packetID")

            if (dataSerializer.readableBytes() > 0) throw IOException("Packet $packetID ($packet) was larger than I expected, found ${dataSerializer.readableBytes()} bytes extra whilst reading packet $packetID")
            out += packet
            ProtocolSwapHandler.swapProtocolIfNeeded(attribute, packet)
        }
    }

    override val supported get() = true
}
