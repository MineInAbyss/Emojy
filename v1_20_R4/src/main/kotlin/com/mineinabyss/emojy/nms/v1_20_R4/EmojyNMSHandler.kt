@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_20_R4

import com.mineinabyss.emojy.*
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.idofront.messaging.broadcastVal
import com.mineinabyss.idofront.plugin.listeners
import com.mineinabyss.idofront.textcomponents.miniMsg
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
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.network.Connection
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.level.block.entity.BlockEntityType
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
                { component -> GlobalTranslator.render(component, locale) }  // encode
            )
        }

        val key = NamespacedKey.fromString("packet_listener", emojy)
        ChannelInitializeListenerHolder.addListener(key!!) { channel: Channel ->
            channel.pipeline().addBefore("packet_handler", key.toString(), object : ChannelDuplexHandler() {
                private val connection = channel.pipeline()["packet_handler"] as Connection
                fun Connection.locale() = player.bukkitEntity.locale()

                override fun write(ctx: ChannelHandlerContext, packet: Any, promise: ChannelPromise) {
                    ctx.write(
                        when (packet) {
                            is ClientboundDisguisedChatPacket -> ClientboundDisguisedChatPacket(packet.message.transformEmotes(connection.locale()), packet.chatType)
                            is ClientboundSystemChatPacket -> ClientboundSystemChatPacket(packet.content.transformEmotes(connection.locale()), packet.overlay)
                            is ClientboundSetTitleTextPacket -> ClientboundSetTitleTextPacket(packet.text.transformEmotes(connection.locale()))
                            is ClientboundSetSubtitleTextPacket -> ClientboundSetSubtitleTextPacket(packet.text.transformEmotes(connection.locale()))
                            is ClientboundSetActionBarTextPacket -> ClientboundSetActionBarTextPacket(packet.text.transformEmotes(connection.locale()))
                            is ClientboundOpenScreenPacket -> ClientboundOpenScreenPacket(packet.containerId, packet.type, packet.title.transformEmotes(connection.locale()))
                            is ClientboundTabListPacket -> ClientboundTabListPacket(packet.header.transformEmotes(connection.locale()), packet.footer.transformEmotes(connection.locale()))
                            is ClientboundSetEntityDataPacket -> ClientboundSetEntityDataPacket(packet.id, packet.packedItems.map {
                                when (val value = it.value) {
                                    is AdventureComponent ->
                                        SynchedEntityData.DataValue(it.id, it.serializer as EntityDataSerializer<AdventureComponent>,
                                            AdventureComponent(PaperAdventure.asAdventure(value).transformEmotes(connection.locale()))
                                        )
                                    else -> it
                                }
                            })
                            is ClientboundBlockEntityDataPacket -> when (packet.type) {
                                BlockEntityType.SIGN -> ClientboundBlockEntityDataPacket(packet.pos, packet.type, packet.tag.apply {
                                    val locale = connection.locale()
                                    val frontText = getCompound("front_text").apply {
                                        put("messages", ListTag()
                                            .apply { addAll(getList("messages", StringTag.TAG_STRING.toInt())
                                                .map { it.asString.drop(1).dropLast(1).miniMsg().transformEmotes(locale) }
                                                .map { m -> StringTag.valueOf(PaperAdventure.asJsonString(m, locale)) })
                                            }
                                        )
                                    }
                                    val backText = getCompound("back_text").apply {
                                        put("messages", ListTag()
                                            .apply { addAll(getList("messages", StringTag.TAG_STRING.toInt())
                                                .map { it.asString.drop(1).dropLast(1).miniMsg().transformEmotes(locale) }
                                                .map { m -> StringTag.valueOf(PaperAdventure.asJsonString(m, locale)) })
                                            }
                                        )
                                    }

                                    put("front_text", frontText)
                                    put("back_text", backText)
                                })
                                else -> packet
                            }
                            else -> packet
                        }, promise
                    )
                }

                override fun channelRead(ctx: ChannelHandlerContext, packet: Any) {
                    ctx.fireChannelRead(when (packet) {
                        is ServerboundRenameItemPacket -> ServerboundRenameItemPacket(packet.name.broadcastVal().miniMsg().escapeEmoteIDs(connection.player.bukkitEntity).serialize())
                        is ServerboundChatPacket -> ServerboundChatPacket(packet.message, packet.timeStamp, packet.salt, packet.signature, packet.lastSeenMessages)
                        else -> packet
                    })
                }
            }
            )
        }
    }

    companion object {

        fun net.minecraft.network.chat.Component.transformEmotes(locale: Locale? = null): net.minecraft.network.chat.Component {
            return PaperAdventure.asVanilla(PaperAdventure.asAdventure(this).transformEmotes(locale))
        }

        fun Component.transformEmotes(locale: Locale? = null): Component {
            var component = GlobalTranslator.render(this, locale ?: Locale.US)
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
