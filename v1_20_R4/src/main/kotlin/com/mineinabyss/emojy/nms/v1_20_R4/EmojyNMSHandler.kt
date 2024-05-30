@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_20_R4

import com.jeff_media.morepersistentdatatypes.DataType
import com.mineinabyss.emojy.*
import com.mineinabyss.emojy.config.SPACE_PERMISSION
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.idofront.items.editItemMeta
import com.mineinabyss.idofront.messaging.broadcastVal
import com.mineinabyss.idofront.messaging.logVal
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
import net.minecraft.world.item.ItemStack
import org.bukkit.Bukkit
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
                            is ClientboundPlayerChatPacket -> ClientboundPlayerChatPacket(packet.sender, packet.index, packet.signature, packet.body, packet.unsignedContent?.transformEmotes(connection.locale(), true)?.unescapeEmoteIds(), packet.filterMask, packet.chatType)
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
                                        AdventureComponent(value.`adventure$component`().transformEmotes(connection.locale()))
                                    )
                                } ?: it
                            })
                            is ClientboundContainerSetSlotPacket -> ClientboundContainerSetSlotPacket(packet.containerId, packet.stateId, packet.slot, packet.item.transformItemNameLore())
                            is ClientboundContainerSetContentPacket -> {
                                val player = connection.player.bukkitEntity
                                (player.openInventory.topInventory as? AnvilInventory)?.let {
                                    ClientboundContainerSetContentPacket(packet.containerId, packet.stateId,
                                        NonNullList.of(packet.items.first(), *packet.items.map {
                                            CraftItemStack.asNMSCopy(CraftItemStack.asBukkitCopy(it).editItemMeta {
                                                setDisplayName(persistentDataContainer.get(ORIGINAL_ITEM_RENAME_TEXT, DataType.STRING) ?: return@editItemMeta)
                                            })
                                        }.toTypedArray()), packet.carriedItem)
                                } ?: ClientboundContainerSetContentPacket(packet.containerId, packet.stateId,
                                    NonNullList.of(packet.items.first(), *packet.items.map { it.transformItemNameLore() }.toTypedArray()),
                                    packet.carriedItem)
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

                private fun ItemStack.transformItemNameLore(): ItemStack {
                    val locale = connection.locale()
                    return CraftItemStack.asNMSCopy(CraftItemStack.asBukkitCopy(this).editItemMeta {
                        itemName(if (hasItemName()) itemName().transformEmotes(locale) else null)
                        lore(lore()?.map { l -> l.transformEmotes(locale) })
                    })
                }
            }
            )
        }
    }

    companion object {

        fun String.transformEmotes(locale: Locale? = null, insert: Boolean = false): String {
            return miniMsg().transformEmotes(locale, insert).serialize()
        }

        fun net.minecraft.network.chat.Component.transformEmotes(locale: Locale? = null, insert: Boolean = false): net.minecraft.network.chat.Component {
            return PaperAdventure.asVanilla(PaperAdventure.asAdventure(this).transformEmotes(locale, insert))
        }

        fun String.escapeEmoteIDs(player: Player?): String {
            return miniMsg().escapeEmoteIDs(player).serialize()
        }

        fun net.minecraft.network.chat.Component.escapeEmoteIDs(player: Player?): net.minecraft.network.chat.Component {
            return PaperAdventure.asVanilla((PaperAdventure.asAdventure(this)).escapeEmoteIDs(player))
        }

        fun net.minecraft.network.chat.Component.unescapeEmoteIds(): net.minecraft.network.chat.Component {
            return PaperAdventure.asVanilla(PaperAdventure.asAdventure(this).unescapeEmoteIds())
        }


    }

    override val supported get() = true
}
