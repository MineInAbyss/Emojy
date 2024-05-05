@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_20_R4

import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.escapeEmoteIDs
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.transformEmoteIDs
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.core.RegistryAccess
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.TagTypes
import net.minecraft.network.*
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.protocol.Packet
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*
import java.util.function.Function

class EmojyNMSHandler : IEmojyNMSHandler {

    private var INFO_BY_ENCODER: Function<PacketEncoder<*>, ProtocolInfo<*>>
    private var INFO_BY_DECODER: Function<PacketDecoder<*>, ProtocolInfo<*>>

    companion object {
        private fun Player.connection() = (this as CraftPlayer).handle.connection.connection
        private fun Player.channel() = (this as CraftPlayer).handle.connection.connection.channel
        private fun registryAccess(): RegistryAccess =
            (emojy.plugin.server as CraftServer).handle.server.registryAccess()
    }

    init {
        val encoder = PacketEncoder::class.java.declaredFields.firstOrNull {
            ProtocolInfo::class.java.isAssignableFrom(it.type)
        }?.apply { isAccessible = true } ?: throw NullPointerException()

        val decoder = PacketDecoder::class.java.declaredFields.firstOrNull {
            ProtocolInfo::class.java.isAssignableFrom(it.type)
        }?.apply { isAccessible = true } ?: throw NullPointerException()

        INFO_BY_ENCODER = Function<PacketEncoder<*>, ProtocolInfo<*>> { e: PacketEncoder<*> ->
            return@Function run { encoder.get(e) } as ProtocolInfo<*>
        }
        INFO_BY_DECODER = Function<PacketDecoder<*>, ProtocolInfo<*>> { e: PacketDecoder<*> ->
            return@Function run { decoder.get(e) } as ProtocolInfo<*>
        }
    }

    override fun inject(player: Player) {
        val channel = player.channel()
        val handler = PlayerHandler(player)
        channel.eventLoop().submit {
            val pipeline = channel.pipeline()
            pipeline.forEach {
                pipeline.replace(
                    it.key, it.key, when (it.value) {
                        is PacketEncoder<*> -> handler.encoder(INFO_BY_ENCODER.apply(it.value as PacketEncoder<*>))
                        is PacketDecoder<*> -> handler.decoder(INFO_BY_DECODER.apply(it.value as PacketDecoder<*>))
                        else -> return@forEach
                    }
                )
            }
        }
    }

    override fun uninject(player: Player) {
        val channel = player.channel()
        val pipeline = channel.pipeline()
        pipeline.forEach {
            pipeline.replace(
                it.key, it.key, when (it.value) {
                    is PlayerHandler.EmojyEncoder -> PacketEncoder((it.value as PlayerHandler.EmojyEncoder).protocolInfo)
                    is PlayerHandler.EmojyDecoder -> PacketDecoder((it.value as PlayerHandler.EmojyDecoder).protocolInfo)
                    else -> return@forEach
                }
            )
        }
    }

    private class EmojyBuffer(val originalBuffer: ByteBuf) : RegistryFriendlyByteBuf(originalBuffer, registryAccess()) {
        override fun copy(): EmojyBuffer {
            return EmojyBuffer(Unpooled.copiedBuffer(originalBuffer))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private data class PlayerHandler(val player: Player) {

        private fun hasComponent(clazz: Class<*>): Boolean {
            return clazz.declaredFields.any { net.minecraft.network.chat.Component::class.java.isAssignableFrom(it.type) }
                    || clazz.declaredFields.any { hasComponent(it::class.java) }
        }

        fun decoder(protocolInfo: ProtocolInfo<*>) = EmojyDecoder(protocolInfo as ProtocolInfo<PacketListener>)
        fun encoder(protocolInfo: ProtocolInfo<*>) = EmojyEncoder(protocolInfo as ProtocolInfo<PacketListener>)
        private fun writeNewNbt(
            original: EmojyBuffer,
            clientbound: Boolean
        ): EmojyBuffer {
            val newBuffer: EmojyBuffer = EmojyBuffer(Unpooled.buffer())
            val id = VarInt.read(original)
            VarInt.write(newBuffer, id)

            val bytes = ByteArray(original.readableBytes())
            original.readBytes(bytes)
            val list: MutableList<Byte> = ArrayList(bytes.size)
            var index = 0
            try {
                loop@ while (index < bytes.size) {
                    val aByte = bytes[index++]
                    list.add(aByte)
                    if (TagTypes.getType(aByte.toInt()) === CompoundTag.TYPE) {
                        val size = bytes.size - index
                        val nbtByte = ByteArray(size)
                        System.arraycopy(bytes, index, nbtByte, 0, nbtByte.size)
                        try {
                            DataInputStream(ByteArrayInputStream(nbtByte)).use { input ->
                                val getTag = CompoundTag.TYPE.load(
                                    input,
                                    NbtAccounter.create(FriendlyByteBuf.DEFAULT_NBT_QUOTA.toLong())
                                )
                                if (getTag.isEmpty) return@use
                                val oldComponent =
                                    PaperAdventure.asAdventure(
                                        ComponentSerialization.CODEC.decode(
                                            NbtOps.INSTANCE,
                                            getTag
                                        ).getOrThrow().first
                                    )
                                val newComponent: Component = when (clientbound) {
                                    true -> oldComponent.transformEmoteIDs(player, true, false)
                                    false -> oldComponent.escapeEmoteIDs(player)
                                }
                                val newTag =
                                    ComponentSerialization.CODEC.encode(
                                        PaperAdventure.asVanilla(newComponent),
                                        NbtOps.INSTANCE,
                                        CompoundTag()
                                    ).getOrThrow() as CompoundTag
                                val stream = ByteArrayOutputStream()
                                DataOutputStream(stream).use { output ->
                                    newTag.write(output)
                                    index += nbtByte.size - input.available()
                                    for (b in stream.toByteArray()) {
                                        list.add(b)
                                    }
                                }
                            }
                        } catch (ignored: Exception) {
                        }
                    }
                }
                val newByte = ByteArray(list.size)
                var i = 0
                for (b in list) newByte[i++] = b
                newBuffer.writeBytes(newByte)
                return newBuffer
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        inner class EmojyEncoder(val protocolInfo: ProtocolInfo<PacketListener>) :
            PacketEncoder<PacketListener>(protocolInfo) {


            override fun encode(ctx: ChannelHandlerContext, packet: Packet<PacketListener>, byteBuf: ByteBuf) {
                if (hasComponent(packet.javaClass)) runCatching {
                    val newBuffer = EmojyBuffer(Unpooled.buffer())
                    protocolInfo.codec().encode(newBuffer, packet)
                    byteBuf.writeBytes(writeNewNbt(newBuffer, true))
                    ProtocolSwapHandler.handleOutboundTerminalPacket(ctx, packet)
                } else super.encode(ctx, packet, byteBuf)
            }
        }

        inner class EmojyDecoder(val protocolInfo: ProtocolInfo<PacketListener>) :
            PacketDecoder<PacketListener>(protocolInfo) {

            override fun decode(ctx: ChannelHandlerContext, byteBuf: ByteBuf, list: MutableList<Any>) {

                runCatching {
                    val packet = protocolInfo.codec().decode(writeNewNbt(EmojyBuffer(byteBuf), false))
                    list += packet
                    ProtocolSwapHandler.handleInboundTerminalPacket(ctx, packet)
                }.onFailure { super.decode(ctx, byteBuf, list) }
            }
        }
    }


    override val supported get() = true
}
