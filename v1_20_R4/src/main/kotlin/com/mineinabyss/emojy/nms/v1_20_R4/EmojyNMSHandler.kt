@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_20_R4

import com.jeff_media.morepersistentdatatypes.DataType
import com.mineinabyss.emojy.*
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.idofront.items.editItemMeta
import com.mineinabyss.idofront.plugin.listeners
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.papermc.paper.adventure.AdventureComponent
import io.papermc.paper.adventure.PaperAdventure
import io.papermc.paper.network.ChannelInitializeListenerHolder
import net.minecraft.core.NonNullList
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.item.ItemStack
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.AnvilInventory
import java.util.*

class EmojyNMSHandler(emojy: EmojyPlugin) : IEmojyNMSHandler {


    init {
        emojy.listeners(EmojyListener())

        val key = NamespacedKey.fromString("packet_listener", emojy)
        ChannelInitializeListenerHolder.addListener(key!!) { channel: Channel ->
            channel.pipeline().addBefore("packet_handler", key.toString(), object : ChannelDuplexHandler() {
                private val connection = channel.pipeline()["packet_handler"] as Connection
                fun Connection.locale() = player.bukkitEntity.locale()

                override fun write(ctx: ChannelHandlerContext, packet: Any, promise: ChannelPromise) {
                    ctx.write(
                        when (packet) {
                            is ClientboundSetScorePacket -> ClientboundSetScorePacket(packet.owner, packet.objectiveName, packet.score, packet.display.map { it.transformEmotes(connection.locale()) }, packet.numberFormat)
                            is ClientboundServerDataPacket -> ClientboundServerDataPacket(packet.motd.transformEmotes(connection.locale()), packet.iconBytes)
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
                            is ClientboundContainerSetContentPacket -> ClientboundContainerSetContentPacket(
                                packet.containerId, packet.stateId, NonNullList.of(packet.items.first(),
                                    *packet.items.map {
                                        val inv = connection.player.bukkitEntity.openInventory.topInventory
                                        val bukkit = CraftItemStack.asBukkitCopy(it)

                                        // If item is firstItem in AnvilInventory we want to set it to have the plain-text displayname
                                        if (inv is AnvilInventory && inv.firstItem == bukkit)
                                            bukkit.itemMeta?.persistentDataContainer?.get(ORIGINAL_ITEM_RENAME_TEXT, DataType.STRING)?.let { og ->
                                                CraftItemStack.asNMSCopy(bukkit.editItemMeta {
                                                    setDisplayName(og)
                                                })
                                            } ?: it.transformItemNameLore()
                                        else it.transformItemNameLore()
                                    }.toTypedArray()), packet.carriedItem)
                            is ClientboundBossEventPacket -> {
                                // Access the private field 'operation'
                                val operationField = ClientboundBossEventPacket::class.java.getDeclaredField("operation").apply { isAccessible = true }
                                val operation = operationField.get(packet)

                                when (operation::class.java.simpleName) {
                                    "AddOperation" -> {
                                        val nameField = operation::class.java.getDeclaredField("name").apply { isAccessible = true }
                                        // Get the component, serialize it and replace "\\<" as it might be escaped if not an AdventureBossbar
                                        val name = PaperAdventure.asAdventure(nameField.get(operation) as Component).serialize().replace("\\<", "<").miniMsg()
                                        nameField.set(operation, PaperAdventure.asVanilla(name.transformEmotes(connection.locale())))
                                    }
                                    "UpdateNameOperation" -> {
                                        val accessorMethod = operation::class.java.methods.find { it.name == "name" }
                                        accessorMethod?.isAccessible = true
                                        if (accessorMethod != null) {
                                            val name = PaperAdventure.asAdventure(accessorMethod.invoke(operation) as Component)
                                                .serialize().replace("\\<", "<")
                                                .miniMsg().transformEmotes(connection.locale())

                                            val updateNameOperationClass = operation::class.java.enclosingClass.declaredClasses.find {
                                                it.simpleName == "UpdateNameOperation"
                                            } ?: throw IllegalStateException("UpdateNameOperation class not found")

                                            val constructor = updateNameOperationClass.getDeclaredConstructor(Component::class.java).apply { isAccessible = true }
                                            // Create a new instance of UpdateNameOperation with the modified name

                                            val updatedOperation = constructor.newInstance(PaperAdventure.asVanilla(name))

                                            // Set the updated operation in the packet
                                            operationField.set(packet, updatedOperation)
                                        }
                                    }
                                }

                                packet
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
                    val player = connection.player.bukkitEntity
                    val locale = player.locale()
                    return CraftItemStack.asNMSCopy(CraftItemStack.asBukkitCopy(this).editItemMeta {
                        itemName(if (hasItemName()) itemName().transformEmotes(locale) else null)
                        lore(lore()?.map { l -> l.transformEmotes(locale) })
                        persistentDataContainer.get(ORIGINAL_ITEM_RENAME_TEXT, DataType.STRING)?.let {
                            displayName(it.miniMsg().escapeEmoteIDs(player).transformEmotes(locale).unescapeEmoteIds())
                        }
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

        fun Component.transformEmotes(locale: Locale? = null, insert: Boolean = false): Component {
            return PaperAdventure.asVanilla(PaperAdventure.asAdventure(this).transformEmotes(locale, insert))
        }

        fun String.escapeEmoteIDs(player: Player?): String {
            return miniMsg().escapeEmoteIDs(player).serialize()
        }

        fun Component.escapeEmoteIDs(player: Player?): Component {
            return PaperAdventure.asVanilla((PaperAdventure.asAdventure(this)).escapeEmoteIDs(player))
        }

        fun Component.unescapeEmoteIds(): Component {
            return PaperAdventure.asVanilla(PaperAdventure.asAdventure(this).unescapeEmoteIds())
        }


    }

    override val supported get() = true
}
