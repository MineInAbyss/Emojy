@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_20_R4

import com.jeff_media.morepersistentdatatypes.DataType
import com.mineinabyss.emojy.*
import com.mineinabyss.emojy.config.SPACE_PERMISSION
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.idofront.items.editItemMeta
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
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.translation.GlobalTranslator
import net.minecraft.core.NonNullList
import net.minecraft.network.Connection
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.SynchedEntityData
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.AnvilInventory
import java.util.*

class EmojyNMSHandler(emojy: EmojyPlugin) : IEmojyNMSHandler {


    init {
        emojy.listeners(EmojyListener())

        val codecs = (PaperAdventure::class.java.getDeclaredField("LOCALIZED_CODECS").apply { isAccessible = true }
            .get(null) as MutableMap<Locale, Codec<Component>>)

        /*for (locale in Locale.getAvailableLocales()) {
            codecs[locale] = AdventureCodecs.COMPONENT_CODEC.xmap(
                { component -> component },  // decode
                { component -> GlobalTranslator.render(component, locale) }  // encode
            )
        }*/

        val key = NamespacedKey.fromString("packet_listener", emojy)
        ChannelInitializeListenerHolder.addListener(key!!) { channel: Channel ->
            channel.pipeline().addBefore("packet_handler", key.toString(), object : ChannelDuplexHandler() {
                private val connection = channel.pipeline()["packet_handler"] as Connection
                fun Connection.locale() = player.bukkitEntity.locale()

                override fun write(ctx: ChannelHandlerContext, packet: Any, promise: ChannelPromise) {
                    ctx.write(
                        when (packet) {
                            is ClientboundDisguisedChatPacket -> ClientboundDisguisedChatPacket(packet.message.transformEmotes(connection.locale(), true).unescapeEmoteIds(), packet.chatType)
                            is ClientboundSystemChatPacket -> ClientboundSystemChatPacket(packet.content.transformEmotes(connection.locale(), true).unescapeEmoteIds(), packet.overlay)
                            is ClientboundSetTitleTextPacket -> ClientboundSetTitleTextPacket(packet.text.transformEmotes(connection.locale()))
                            is ClientboundSetSubtitleTextPacket -> ClientboundSetSubtitleTextPacket(packet.text.transformEmotes(connection.locale()))
                            is ClientboundSetActionBarTextPacket -> ClientboundSetActionBarTextPacket(packet.text.transformEmotes(connection.locale()))
                            is ClientboundOpenScreenPacket -> ClientboundOpenScreenPacket(packet.containerId, packet.type, packet.title.transformEmotes(connection.locale()))
                            is ClientboundTabListPacket -> ClientboundTabListPacket(packet.header.transformEmotes(connection.locale()), packet.footer.transformEmotes(connection.locale()))
                            is ClientboundSetEntityDataPacket -> ClientboundSetEntityDataPacket(packet.id, packet.packedItems.map {
                                (it.value as? AdventureComponent)?.let { value ->
                                    SynchedEntityData.DataValue(it.id, it.serializer as EntityDataSerializer<AdventureComponent>,
                                        AdventureComponent(PaperAdventure.asAdventure(value).transformEmotes(connection.locale()))
                                    )
                                } ?: it
                            })
                            is ClientboundContainerSetContentPacket -> {
                                (connection.player.bukkitEntity.openInventory.topInventory as? AnvilInventory)?.let {
                                    ClientboundContainerSetContentPacket(packet.containerId, packet.stateId,
                                        NonNullList.of(packet.items.first(), *packet.items.map {
                                            CraftItemStack.asNMSCopy(CraftItemStack.asBukkitCopy(it.copy()).editItemMeta {
                                                setDisplayName(persistentDataContainer.get(ORIGINAL_ITEM_RENAME_TEXT, DataType.STRING) ?: return@editItemMeta)
                                            })
                                        }.toTypedArray()), packet.carriedItem)
                                } ?: packet
                            }
                            else -> packet
                        }, promise
                    )
                }

                override fun channelRead(ctx: ChannelHandlerContext, packet: Any) {
                    ctx.fireChannelRead(when (packet) {
                        is ServerboundRenameItemPacket -> ServerboundRenameItemPacket(packet.name.escapeEmoteIDs(connection.player.bukkitEntity))
                        else -> packet
                    })
                }
            }
            )
        }
    }

    companion object {

        val ORIGINAL_SIGN_FRONT_LINES = NamespacedKey.fromString("emojy:original_front_lines")!!
        val ORIGINAL_SIGN_BACK_LINES = NamespacedKey.fromString("emojy:original_back_lines")!!
        val ORIGINAL_ITEM_RENAME_TEXT = NamespacedKey.fromString("emojy:original_item_rename")!!

        fun String.transformEmotes(locale: Locale? = null, insert: Boolean = false): String {
            return miniMsg().transformEmotes(locale, insert).serialize()
        }

        fun net.minecraft.network.chat.Component.transformEmotes(locale: Locale? = null, insert: Boolean = false): net.minecraft.network.chat.Component {
            return PaperAdventure.asVanilla(PaperAdventure.asAdventure(this).transformEmotes(locale, insert))
        }

        fun Component.transformEmotes(locale: Locale? = null, insert: Boolean = false): Component {
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
                                insert = insert,
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

        fun String.escapeEmoteIDs(player: Player?): String {
            return miniMsg().escapeEmoteIDs(player).serialize()
        }

        fun net.minecraft.network.chat.Component.escapeEmoteIDs(player: Player?): net.minecraft.network.chat.Component {
            return PaperAdventure.asVanilla((PaperAdventure.asAdventure(this)).escapeEmoteIDs(player))
        }

        fun Component.escapeEmoteIDs(player: Player?): Component {
            var component = this
            val serialized = component.serialize()

            // Replace all unicodes found in default font with a random one
            // This is to prevent use of unicodes from the font the chat is in
            val (defaultKey, randomKey) = Key.key("default") to Key.key("random")
            for (emote in emojy.emotes.filter { it.font == defaultKey && !it.checkPermission(player) }) emote.unicodes.forEach {
                component = component.replaceText(
                    TextReplacementConfig.builder()
                        .matchLiteral(it)
                        .replacement(it.miniMsg().font(randomKey))
                        .build()
                )
            }

            for (emote in emojy.emotes) emote.baseRegex.findAll(serialized).forEach { match ->
                if (emote.checkPermission(player)) return@forEach

                component = component.replaceText(
                    TextReplacementConfig.builder()
                        .matchLiteral(match.value).once()
                        .replacement("\\${match.value}".miniMsg())
                        .build()
                )
            }

            for (gif in emojy.gifs) gif.baseRegex.findAll(serialized).forEach { match ->
                if (gif.checkPermission(player)) return@forEach
                component = component.replaceText(
                    TextReplacementConfig.builder()
                        .matchLiteral(match.value).once()
                        .replacement("\\${match.value}".miniMsg())
                        .build()
                )
            }

            spaceRegex.findAll(serialized).forEach { match ->
                if (player?.hasPermission(SPACE_PERMISSION) != false) return@forEach
                val space = match.groupValues[1].toIntOrNull() ?: return@forEach

                component = component.replaceText(
                    TextReplacementConfig.builder()
                        .matchLiteral(match.value).once()
                        .replacement("\\:space_$space:".miniMsg())
                        .build()
                )
            }

            return component
        }

        fun net.minecraft.network.chat.Component.unescapeEmoteIds(): net.minecraft.network.chat.Component {
            return PaperAdventure.asVanilla(PaperAdventure.asAdventure(this).unescapeEmoteIds())
        }

        fun Component.unescapeEmoteIds(): Component {
            var component = this
            val serialized = this.serialize()

            for (emote in emojy.emotes) emote.escapedRegex.findAll(serialized).forEach { match ->
                component = component.replaceText(
                    TextReplacementConfig.builder()
                        .match(emote.escapedRegex.pattern).once()
                        .replacement(match.value.removePrefix("\\"))
                        .build()
                )
            }

            for (gif in emojy.gifs) gif.escapedRegex.findAll(serialized).forEach { match ->
                component = component.replaceText(
                    TextReplacementConfig.builder()
                        .match(gif.escapedRegex.pattern).once()
                        .replacement(match.value.removePrefix("\\"))
                        .build()
                )
            }

            escapedSpaceRegex.findAll(serialized).forEach { match ->
                component = component.replaceText(
                    TextReplacementConfig.builder()
                        .match(match.value).once()
                        .replacement(match.value.removePrefix("\\"))
                        .build()
                )
            }

            return component
        }
    }

    override val supported get() = true
}
