@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_20_R4

import com.mineinabyss.emojy.*
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.idofront.plugin.listeners
import com.mineinabyss.idofront.textcomponents.serialize
import com.mojang.serialization.Codec
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.papermc.paper.adventure.AdventureCodecs
import io.papermc.paper.adventure.AdventureComponent
import io.papermc.paper.adventure.PaperAdventure
import io.papermc.paper.network.ChannelInitializeListenerHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.translation.GlobalTranslator
import net.minecraft.network.Connection
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.SynchedEntityData
import org.bukkit.NamespacedKey
import java.util.*

class EmojyNMSHandler(emojy: EmojyPlugin) : IEmojyNMSHandler {

    override val locals: MutableSet<Locale> = mutableSetOf()

    init {
        emojy.listeners(EmojyListener())

        val codecs = (PaperAdventure::class.java.getDeclaredField("LOCALIZED_CODECS").apply { isAccessible = true }
            .get(null) as MutableMap<Locale, Codec<Component>>)

        for (locale in Locale.getAvailableLocales()) {
            codecs[locale] = AdventureCodecs.COMPONENT_CODEC.xmap(
                { component -> component },  // decode
                { component -> GlobalTranslator.render(component.transformEmotes(), locale) }  // encode
            )
        }

        val key = NamespacedKey.fromString("configuration_listener", emojy)
        ChannelInitializeListenerHolder.addListener(key!!) { channel: Channel ->
            channel.pipeline().addBefore("packet_handler", key.toString(), object : ChannelDuplexHandler() {
                private val connection = channel.pipeline()["packet_handler"] as Connection

                override fun write(ctx: ChannelHandlerContext, packet: Any, promise: ChannelPromise) {
                    ctx.write(
                        when (packet) {
                            is ClientboundSetTitleTextPacket -> ClientboundSetTitleTextPacket(packet.text.transformEmotes())
                            is ClientboundSetSubtitleTextPacket -> ClientboundSetSubtitleTextPacket(packet.text.transformEmotes())
                            is ClientboundSetActionBarTextPacket -> ClientboundSetActionBarTextPacket(packet.text.transformEmotes())
                            is ClientboundOpenScreenPacket -> ClientboundOpenScreenPacket(packet.containerId, packet.type, packet.title.transformEmotes())
                            is ClientboundSetEntityDataPacket -> ClientboundSetEntityDataPacket(packet.id, packet.packedItems.map {
                                when (val value = it.value) {
                                    is AdventureComponent ->
                                        SynchedEntityData.DataValue(it.id, it.serializer as EntityDataSerializer<AdventureComponent>,
                                            AdventureComponent(PaperAdventure.asAdventure(value.transformEmotes()))
                                        )
                                    else -> it
                                }
                            })
                            else -> packet
                        }, promise
                    )
                }

                /*override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                    if (msg is ServerboundResourcePackPacket && msg.id() == OraxenPlugin.get().packServer()
                            .packInfo().id()
                    ) {
                        if (!finishConfigPhase(msg.action())) return@addListener
                        ctx.pipeline()
                            .remove(this) // We no longer need to listen or process ClientboundFinishConfigurationPacket that we send ourselves
                        connection.send(ClientboundFinishConfigurationPacket.INSTANCE)
                        return@addListener
                    }
                    ctx.fireChannelRead(msg)
                }*/
            }
            )
        }
    }

    companion object {

        fun net.minecraft.network.chat.Component.transformEmotes(): net.minecraft.network.chat.Component {
            return PaperAdventure.asVanilla(PaperAdventure.asAdventure(this).transformEmotes())
        }

        fun Component.transformEmotes(): Component {
            var component = this
            val serialized = this.serialize()

            for (emote in emojy.emotes) emote.baseRegex.findAll(serialized).forEach { match ->

                val colorable = colorableRegex in match.value
                val bitmapIndex = bitmapIndexRegex.find(match.value)?.groupValues?.get(1)?.toIntOrNull() ?: -1

                component = component.replaceText(
                    TextReplacementConfig.builder()
                        .match(emote.baseRegex.pattern).once()
                        .replacement(
                            emote.formattedUnicode(
                                insert = false,
                                colorable = colorable,
                                bitmapIndex = bitmapIndex
                            )
                        )
                        .build()
                )
            }

            for (gif in emojy.gifs) gif.baseRegex.findAll(serialized).forEach { _ ->
                component = component.replaceText(
                    TextReplacementConfig.builder()
                        .match(gif.baseRegex.pattern).once()
                        .replacement(gif.formattedUnicode(insert = false))
                        .build()
                )
            }

            spaceRegex.findAll(serialized).forEach { match ->
                val space = match.groupValues[1].toIntOrNull() ?: return@forEach
                val spaceRegex = "(?<!\\\\):space_(-?$space+):".toRegex()
                component = component.replaceText(
                    TextReplacementConfig.builder()
                        .match(spaceRegex.pattern).once()
                        .replacement(spaceComponent(space))
                        .build()
                )
            }

            return component
        }
    }

    override val supported get() = true
}
