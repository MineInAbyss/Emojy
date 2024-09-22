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
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
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

                override fun write(ctx: ChannelHandlerContext, packet: Any, promise: ChannelPromise) {
                    ctx.write(transformPacket(packet, connection), promise)
                }

                override fun channelRead(ctx: ChannelHandlerContext, packet: Any) {
                    ctx.fireChannelRead(when (packet) {
                        is ServerboundRenameItemPacket -> ServerboundRenameItemPacket(packet.name.escapeEmoteIDs(connection.player.bukkitEntity))
                        else -> packet
                    })
                }
            })
        }
    }

    companion object {

        private fun Connection.locale() = player.bukkitEntity.locale()
        fun transformPacket(packet: Any, connection: Connection): Any {
            return when (packet) {
                is ClientboundBundlePacket -> ClientboundBundlePacket(packet.subPackets().map { transformPacket(it, connection) as Packet<in ClientGamePacketListener> })
                is ClientboundSetScorePacket -> ClientboundSetScorePacket(packet.owner, packet.objectiveName, packet.score, packet.display.map { it.transformEmotes(connection.locale()) }, packet.numberFormat)
                is ClientboundServerDataPacket -> ClientboundServerDataPacket(packet.motd.transformEmotes(connection.locale()), packet.iconBytes)
                is ClientboundDisguisedChatPacket -> ClientboundDisguisedChatPacket(packet.message.transformEmotes(connection.locale(), true).unescapeEmoteIds(), packet.chatType)
                is ClientboundPlayerChatPacket -> ClientboundPlayerChatPacket(packet.sender, packet.index, packet.signature, packet.body, (packet.unsignedContent ?: PaperAdventure.asVanilla(packet.body.content.miniMsg()))?.transformEmotes(connection.locale(), true)?.unescapeEmoteIds(), packet.filterMask, ChatType.bind(packet.chatType.chatType.unwrapKey().get(), connection.player.registryAccess(), packet.chatType.name.transformEmotes(connection.locale(), true)))
                is ClientboundSystemChatPacket -> ClientboundSystemChatPacket(packet.content.transformEmotes(connection.locale(), true).unescapeEmoteIds(), packet.overlay)
                is ClientboundSetTitleTextPacket -> ClientboundSetTitleTextPacket(packet.text.transformEmotes(connection.locale()))
                is ClientboundSetSubtitleTextPacket -> ClientboundSetSubtitleTextPacket(packet.text.transformEmotes(connection.locale()))
                is ClientboundSetActionBarTextPacket -> ClientboundSetActionBarTextPacket(packet.text.transformEmotes(connection.locale()))
                is ClientboundOpenScreenPacket -> ClientboundOpenScreenPacket(packet.containerId, packet.type, packet.title.transformEmotes(connection.locale()))
                is ClientboundTabListPacket -> ClientboundTabListPacket(packet.header.transformEmotes(connection.locale()), packet.footer.transformEmotes(connection.locale()))
                is ClientboundResourcePackPushPacket -> ClientboundResourcePackPushPacket(packet.id, packet.url, packet.hash, packet.required, packet.prompt.map { it.transformEmotes(connection.locale()) })
                is ClientboundDisconnectPacket -> ClientboundDisconnectPacket(packet.reason.transformEmotes(connection.locale()))
                is ClientboundSetEntityDataPacket -> ClientboundSetEntityDataPacket(packet.id, packet.packedItems.map {
                    (it.value as? AdventureComponent)?.let { value ->
                        SynchedEntityData.DataValue(it.id, it.serializer as EntityDataSerializer<AdventureComponent>,
                            AdventureComponent(value.`adventure$component`().transformEmotes(connection.locale()))
                        )
                    } ?: it
                })
                is ClientboundContainerSetSlotPacket -> ClientboundContainerSetSlotPacket(packet.containerId, packet.stateId, packet.slot, packet.item.transformItemNameLore(connection.player.bukkitEntity))
                is ClientboundContainerSetContentPacket -> ClientboundContainerSetContentPacket(
                    packet.containerId, packet.stateId, NonNullList.of(packet.items.first(),
                        *packet.items.map {
                            val player = connection.player.bukkitEntity
                            val inv = player.openInventory.topInventory
                            val bukkit = CraftItemStack.asBukkitCopy(it)

                            // If item is firstItem in AnvilInventory we want to set it to have the plain-text displayname
                            if (inv is AnvilInventory && inv.firstItem == bukkit)
                                bukkit.itemMeta?.persistentDataContainer?.get(ORIGINAL_ITEM_RENAME_TEXT, DataType.STRING)?.let { og ->
                                    CraftItemStack.asNMSCopy(bukkit.apply { editMeta { it.setDisplayName(og) } })
                                } ?: it.transformItemNameLore(player)
                            else it.transformItemNameLore(player)
                        }.toTypedArray()), packet.carriedItem)
                is ClientboundBossEventPacket -> {
                    // Access the private field 'operation'
                    val operationField = ClientboundBossEventPacket::class.java.getDeclaredField("operation").apply { isAccessible = true }
                    val operation = operationField.get(packet)

                    when (operation::class.java.simpleName) {
                        "AddOperation" -> {
                            val nameField = operation::class.java.getDeclaredField("name").apply { isAccessible = true }
                            nameField.set(operation, (nameField.get(operation) as Component).transformEmotes(connection.locale()))
                        }
                        "UpdateNameOperation" -> {
                            val accessorMethod = operation::class.java.methods.find { it.name == "name" }
                            accessorMethod?.isAccessible = true
                            if (accessorMethod != null) {
                                val updateNameOperationClass = operation::class.java.enclosingClass.declaredClasses.find {
                                    it.simpleName == "UpdateNameOperation"
                                } ?: throw IllegalStateException("UpdateNameOperation class not found")

                                val constructor = updateNameOperationClass.getDeclaredConstructor(Component::class.java).apply { isAccessible = true }
                                val name = (accessorMethod.invoke(operation) as Component).transformEmotes(connection.locale())
                                val updatedOperation = constructor.newInstance(name)

                                operationField.set(packet, updatedOperation)
                            }
                        }
                    }

                    packet
                }
                is ClientboundPlayerInfoUpdatePacket ->
                    ClientboundPlayerInfoUpdatePacket(packet.actions(), packet.entries().map {
                        ClientboundPlayerInfoUpdatePacket.Entry(
                            it.profileId, it.profile, it.listed, it.latency, it.gameMode,
                            it.displayName?.transformEmotes(connection.locale()), it.chatSession
                        )
                    })
                else -> packet
            }
        }

        private fun ItemStack.transformItemNameLore(player: Player): ItemStack {
            val locale = player.locale()
            return CraftItemStack.asNMSCopy(CraftItemStack.asBukkitCopy(this).apply {
                editMeta { meta ->
                    meta.itemName(if (meta.hasItemName()) meta.itemName().transformEmotes(locale) else null)
                    lore(lore()?.map { l -> l.transformEmotes(locale) })
                    meta.persistentDataContainer.get(ORIGINAL_ITEM_RENAME_TEXT, DataType.STRING)?.let {
                        meta.displayName(it.escapeEmoteIDs(player).transformEmotes().unescapeEmoteIds().miniMsg())
                    }

                }
            })
        }

        fun Component.transformEmotes(locale: Locale? = null, insert: Boolean = false): Component {
            return when {
                // Sometimes a NMS component is partially Literal, so ensure entire thing is just one LiteralContent with no extra data
                contents is LiteralContents && style.isEmpty && siblings.isEmpty() -> (contents as LiteralContents).text.miniMsg()
                else -> PaperAdventure.asAdventure(this)
            }.transformEmotes(locale, insert).let(PaperAdventure::asVanilla)
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
