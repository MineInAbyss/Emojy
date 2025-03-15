@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_21_R2

import com.jeff_media.morepersistentdatatypes.DataType
import com.mineinabyss.emojy.EmojyPlugin
import com.mineinabyss.emojy.ORIGINAL_ITEM_RENAME_TEXT
import com.mineinabyss.emojy.escapeEmoteIDs
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.transformEmotes
import com.mineinabyss.emojy.unescapeEmoteIds
import com.mineinabyss.idofront.items.editItemMeta
import com.mineinabyss.idofront.plugin.listeners
import com.mineinabyss.idofront.textcomponents.miniMsg
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.papermc.paper.adventure.AdventureComponent
import io.papermc.paper.adventure.PaperAdventure
import io.papermc.paper.network.ChannelInitializeListenerHolder
import java.util.Locale
import java.util.Optional
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPredicate
import net.minecraft.core.component.DataComponents
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.network.Connection
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundServerDataPacket
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ClientboundTabListPacket
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.AnvilInventory
import kotlin.jvm.optionals.getOrNull

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
            }
            )
        }
    }

    companion object {
        private fun Connection.locale() = player.bukkitEntity.locale()
        fun transformPacket(packet: Any, connection: Connection): Any {
            return when (packet) {
                is ClientboundBundlePacket -> ClientboundBundlePacket(packet.subPackets().map { transformPacket(it, connection) as Packet<in ClientGamePacketListener> })
                is ClientboundServerLinksPacket -> ClientboundServerLinksPacket(packet.links.map { net.minecraft.server.ServerLinks.UntrustedEntry(it.type.mapRight { it.transformEmotes(connection.locale()) }, it.link) })
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
                    when (val value = it.value) {
                        is AdventureComponent -> SynchedEntityData.DataValue(it.id, it.serializer as EntityDataSerializer<AdventureComponent>,
                            AdventureComponent(value.`adventure$component`().transformEmotes(connection.locale()))
                        )
                        is Component -> SynchedEntityData.DataValue(it.id, EntityDataSerializers.COMPONENT, value.transformEmotes(connection.locale()))
                        is Optional<*> -> when (val comp = value.getOrNull()) {
                            is AdventureComponent -> SynchedEntityData.DataValue(it.id, it.serializer as EntityDataSerializer<Optional<AdventureComponent>>,
                                Optional.of(AdventureComponent(comp.`adventure$component`().transformEmotes(connection.locale())))
                            )
                            is Component -> SynchedEntityData.DataValue(it.id, EntityDataSerializers.OPTIONAL_COMPONENT, Optional.of(comp.transformEmotes(connection.locale())))
                            else -> it
                        }
                        else -> it
                    }
                })
                is ClientboundPlayerInfoUpdatePacket -> ClientboundPlayerInfoUpdatePacket(packet.actions(), packet.entries().map {
                    ClientboundPlayerInfoUpdatePacket.Entry(
                        it.profileId, it.profile, it.listed, it.latency, it.gameMode,
                        it.displayName?.transformEmotes(connection.locale()), it.listOrder, it.chatSession
                    )
                })
                is ClientboundContainerSetSlotPacket -> ClientboundContainerSetSlotPacket(packet.containerId, packet.stateId, packet.slot, packet.item.transformItemNameLore(connection.player.bukkitEntity))
                is ClientboundMerchantOffersPacket -> ClientboundMerchantOffersPacket(packet.containerId, MerchantOffers().apply {
                    val player = connection.player.bukkitEntity
                    fun ItemCost.transform() = ItemCost(item, count, components.transformItemNameLore(player), itemStack)

                    addAll(packet.offers.map { offer ->
                        MerchantOffer(
                            offer.baseCostA.transform(), offer.costB.map { it.transform() }, offer.result.transformItemNameLore(player),
                            offer.uses, offer.maxUses, offer.xp, offer.priceMultiplier, offer.demand, offer.ignoreDiscounts, offer.asBukkit()
                        )
                    })
                }, packet.villagerLevel, packet.villagerXp, packet.showProgress(), packet.canRestock())
                is ClientboundContainerSetContentPacket -> ClientboundContainerSetContentPacket(
                    packet.containerId, packet.stateId, packet.items.mapTo(NonNullList.create()) { item ->
                        val player = connection.player.bukkitEntity
                        val inv = player.openInventory.topInventory
                        val bukkit = CraftItemStack.asBukkitCopy(item)

                        // If item is firstItem in AnvilInventory we want to set it to have the plain-text displayname
                        if (inv is AnvilInventory && inv.firstItem == bukkit) {
                            item.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getCompound("PublicBukkitValues")?.getString(ORIGINAL_ITEM_RENAME_TEXT.toString())?.let { og ->
                                item.copy().apply { set(DataComponents.CUSTOM_NAME, Component.literal(og)) }
                            } ?: item.transformItemNameLore(player)
                        } else item.transformItemNameLore(player)
                    }, packet.carriedItem)
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
                else -> packet
            }
        }

        private fun ItemStack.transformItemNameLore(player: Player): ItemStack {
            return copy().apply {
                val locale = player.locale()
                set(DataComponents.ITEM_NAME, get(DataComponents.ITEM_NAME)?.transformEmotes(locale))
                set(DataComponents.LORE, get(DataComponents.LORE)?.let { itemLore ->
                    ItemLore(itemLore.lines.map { Component.empty().setStyle(Style.EMPTY.withItalic(false)).append(it.transformEmotes(locale)) }, itemLore.styledLines.map { Component.empty().setStyle(Style.EMPTY).append(it.transformEmotes(locale)) })
                })
                get(DataComponents.CUSTOM_DATA)?.copyTag()?.getCompound("PublicBukkitValues")?.getString(ORIGINAL_ITEM_RENAME_TEXT.toString())?.takeIf { it.isNotEmpty() }?.let {
                    set(DataComponents.CUSTOM_NAME, PaperAdventure.asVanilla(it.escapeEmoteIDs(player).transformEmotes().unescapeEmoteIds().miniMsg()))
                }
            }
        }

        private fun DataComponentPredicate.transformItemNameLore(player: Player): DataComponentPredicate {
            val locale = player.locale()
            val map = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, asPatch())

            map.set(DataComponents.ITEM_NAME, map.get(DataComponents.ITEM_NAME)?.transformEmotes(locale))
            map.set(DataComponents.LORE, map.get(DataComponents.LORE)?.let { itemLore ->
                ItemLore(itemLore.lines.map { Component.empty().setStyle(Style.EMPTY.withItalic(false)).append(it.transformEmotes(locale)) }, itemLore.styledLines.map { Component.empty().setStyle(Style.EMPTY).append(it.transformEmotes(locale)) })
            })
            map.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getCompound("PublicBukkitValues")?.getString(ORIGINAL_ITEM_RENAME_TEXT.toString())?.takeIf { it.isNotEmpty() }?.let {
                map.set(DataComponents.CUSTOM_NAME, PaperAdventure.asVanilla(it.escapeEmoteIDs(player).transformEmotes().unescapeEmoteIds().miniMsg()))
            }

            return DataComponentPredicate.allOf(map)
        }

        fun Component.transformEmotes(locale: Locale? = null, insert: Boolean = false): Component {
            return when {
                this is AdventureComponent -> this.`adventure$component`()
                // Sometimes a NMS component is partially Literal, so ensure entire thing is just one LiteralContent with no extra data
                contents is LiteralContents && style.isEmpty && siblings.isEmpty() ->
                    (contents as LiteralContents).text.let { it.takeUnless { "§" in it }?.miniMsg() ?: IEmojyNMSHandler.legacyHandler.deserialize(it) }
                contents is TranslatableContents -> {
                    val contents = contents as TranslatableContents
                    val args = contents.args.map { (it as? Component)?.transformEmotes(locale) ?: it }.toTypedArray()
                    return MutableComponent.create(TranslatableContents(contents.key, contents.fallback, args)).setStyle(style).apply {
                        siblings.map { it.transformEmotes(locale) }.forEach(::append)
                    }
                }
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
