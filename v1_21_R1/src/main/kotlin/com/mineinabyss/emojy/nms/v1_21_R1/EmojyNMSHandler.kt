@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_21_R1

import com.mineinabyss.emojy.EmojyPlugin
import com.mineinabyss.emojy.ORIGINAL_ITEM_RENAME_TEXT
import com.mineinabyss.emojy.escapeEmoteIDs
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.transformEmotes
import com.mineinabyss.emojy.unescapeEmoteIds
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
import kotlin.reflect.KClass

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
        private val packetTransformers: Map<KClass<out Packet<*>>, (Packet<*>, Connection) -> Packet<*>> = mapOf(
            ClientboundBundlePacket::class to { p, conn ->
                p as ClientboundBundlePacket
                ClientboundBundlePacket(p.subPackets().map { transformPacket(it, conn) as Packet<in ClientGamePacketListener> })
            },
            ClientboundServerLinksPacket::class to { p, conn ->
                p as ClientboundServerLinksPacket
                ClientboundServerLinksPacket(p.links.map { net.minecraft.server.ServerLinks.UntrustedEntry(it.type.mapRight { it.transformEmotes(conn.locale()) }, it.link) })
            },
            ClientboundSetScorePacket::class to { p, conn ->
                p as ClientboundSetScorePacket
                ClientboundSetScorePacket(p.owner, p.objectiveName, p.score, p.display.map { it.transformEmotes(conn.locale()) }, p.numberFormat)
            },
            ClientboundServerDataPacket::class to { p, conn ->
                p as ClientboundServerDataPacket
                ClientboundServerDataPacket(p.motd.transformEmotes(conn.locale()), p.iconBytes)
            },
            ClientboundDisguisedChatPacket::class to { p, conn ->
                p as ClientboundDisguisedChatPacket
                ClientboundDisguisedChatPacket(p.message.transformEmotes(conn.locale(), true).unescapeEmoteIds(), p.chatType)
            },
            ClientboundPlayerChatPacket::class to { p, conn ->
                p as ClientboundPlayerChatPacket
                ClientboundPlayerChatPacket(p.sender, p.index, p.signature, p.body,
                    (p.unsignedContent ?: PaperAdventure.asVanilla(p.body.content.miniMsg()))?.transformEmotes(conn.locale(), true)?.unescapeEmoteIds(),
                    p.filterMask, ChatType.bind(p.chatType.chatType.unwrapKey().get(), conn.player.registryAccess(), p.chatType.name.transformEmotes(conn.locale(), true))
                )
            },
            ClientboundSystemChatPacket::class to { p, conn ->
                p as ClientboundSystemChatPacket
                ClientboundSystemChatPacket(p.content.transformEmotes(conn.locale(), true).unescapeEmoteIds(), p.overlay)
            },
            ClientboundSetTitleTextPacket::class to { p, conn ->
                p as ClientboundSetTitleTextPacket
                ClientboundSetTitleTextPacket(p.text.transformEmotes(conn.locale()))
            },
            ClientboundSetSubtitleTextPacket::class to { p, conn ->
                p as ClientboundSetSubtitleTextPacket
                ClientboundSetSubtitleTextPacket(p.text.transformEmotes(conn.locale()))
            },
            ClientboundSetActionBarTextPacket::class to { p, conn ->
                p as ClientboundSetActionBarTextPacket
                ClientboundSetActionBarTextPacket(p.text.transformEmotes(conn.locale()))
            },
            ClientboundOpenScreenPacket::class to { p, conn ->
                p as ClientboundOpenScreenPacket
                ClientboundOpenScreenPacket(p.containerId, p.type, p.title.transformEmotes(conn.locale()))
            },
            ClientboundTabListPacket::class to { p, conn ->
                p as ClientboundTabListPacket
                ClientboundTabListPacket(p.header.transformEmotes(conn.locale()), p.footer.transformEmotes(conn.locale()))
            },
            ClientboundResourcePackPushPacket::class to { p, conn ->
                p as ClientboundResourcePackPushPacket
                ClientboundResourcePackPushPacket(p.id, p.url, p.hash, p.required, p.prompt.map { it.transformEmotes(conn.locale()) })
            },
            ClientboundDisconnectPacket::class to { p, conn ->
                p as ClientboundDisconnectPacket
                ClientboundDisconnectPacket(p.reason.transformEmotes(conn.locale()))
            },
            ClientboundSetEntityDataPacket::class to { p, conn ->
                p as ClientboundSetEntityDataPacket
                ClientboundSetEntityDataPacket(p.id, p.packedItems.map {
                    when (val value = it.value) {
                        is AdventureComponent -> SynchedEntityData.DataValue(it.id, it.serializer as EntityDataSerializer<AdventureComponent>,
                            AdventureComponent(value.`adventure$component`().transformEmotes(conn.locale()))
                        )
                        is Component -> SynchedEntityData.DataValue(it.id, EntityDataSerializers.COMPONENT, value.transformEmotes(conn.locale()))
                        is Optional<*> -> when (val comp = value.getOrNull()) {
                            is AdventureComponent -> SynchedEntityData.DataValue(it.id, it.serializer as EntityDataSerializer<Optional<AdventureComponent>>,
                                Optional.of(AdventureComponent(comp.`adventure$component`().transformEmotes(conn.locale())))
                            )
                            is Component -> SynchedEntityData.DataValue(it.id, EntityDataSerializers.OPTIONAL_COMPONENT, Optional.of(comp.transformEmotes(conn.locale())))
                            else -> it
                        }
                        else -> it
                    }
                })
            },
            ClientboundPlayerInfoUpdatePacket::class to { p, conn ->
                p as ClientboundPlayerInfoUpdatePacket
                ClientboundPlayerInfoUpdatePacket(p.actions(), p.entries().map {
                    ClientboundPlayerInfoUpdatePacket.Entry(
                        it.profileId, it.profile, it.listed, it.latency, it.gameMode,
                        it.displayName?.transformEmotes(conn.locale()), it.chatSession
                    )
                })
            },
            ClientboundContainerSetSlotPacket::class to { p, conn ->
                p as ClientboundContainerSetSlotPacket
                ClientboundContainerSetSlotPacket(p.containerId, p.stateId, p.slot, p.item.transformItemNameLore(conn.player.bukkitEntity))
            },
            ClientboundMerchantOffersPacket::class to { p, conn ->
                p as ClientboundMerchantOffersPacket
                ClientboundMerchantOffersPacket(p.containerId, MerchantOffers().apply {
                    val player = conn.player.bukkitEntity
                    fun ItemCost.transform() = ItemCost(item, count, components.transformItemNameLore(player), itemStack)

                    addAll(p.offers.map { offer ->
                        MerchantOffer(
                            offer.baseCostA.transform(), offer.costB.map { it.transform() }, offer.result.transformItemNameLore(player),
                            offer.uses, offer.maxUses, offer.xp, offer.priceMultiplier, offer.demand, offer.ignoreDiscounts, offer.asBukkit()
                        )
                    })
                }, p.villagerLevel, p.villagerXp, p.showProgress(), p.canRestock())
            },
            ClientboundContainerSetContentPacket::class to { p, conn ->
                p as ClientboundContainerSetContentPacket
                ClientboundContainerSetContentPacket(
                    p.containerId, p.stateId, p.items.mapTo(NonNullList.create()) { item ->
                        val player = conn.player.bukkitEntity
                        val inv = player.openInventory.topInventory
                        val bukkit = CraftItemStack.asBukkitCopy(item)

                        if (inv is AnvilInventory && inv.firstItem == bukkit) {
                            item.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getCompound("PublicBukkitValues")?.getString(ORIGINAL_ITEM_RENAME_TEXT.toString())?.let { og ->
                                item.copy().apply { set(DataComponents.CUSTOM_NAME, Component.literal(og)) }
                            } ?: item.transformItemNameLore(player)
                        } else item.transformItemNameLore(player)
                    }, p.carriedItem
                )
            },
            ClientboundBossEventPacket::class to { p, conn -> transformBossEventPacket(p as ClientboundBossEventPacket, conn) }
        )

        fun transformPacket(packet: Any, connection: Connection): Any {
            if (packet !is Packet<*>) return packet
            return packetTransformers[packet::class]?.invoke(packet, connection) ?: packet
        }

        private val bossEventOperationField = ClientboundBossEventPacket::class.java.getDeclaredField("operation").apply { isAccessible = true }
        private val bossEventAddOperation = Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket.AddOperation")
        private val bossEventAddOperationAccessorMethod = bossEventAddOperation::class.java.getDeclaredField("name").apply { isAccessible = true }
        private val bossEventUpdateNameOperation = Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket.UpdateNameOperation")
        private val bossEventUpdateNameOperationAccessorMethod = bossEventUpdateNameOperation::class.java.methods.find { it.name == "name" }?.apply { isAccessible = true }
        private val bossEventUpdateNameOperationComponentField = bossEventUpdateNameOperation::class.java.getDeclaredConstructor(Component::class.java).apply { isAccessible = true }
        private fun transformBossEventPacket(packet: ClientboundBossEventPacket, connection: Connection): ClientboundBossEventPacket {

            val operation = bossEventOperationField.get(packet)

            when (operation::class.java.simpleName) {
                "AddOperation" ->
                    bossEventAddOperationAccessorMethod.set(operation, (bossEventAddOperationAccessorMethod.get(operation) as Component).transformEmotes(connection.locale()))
                "UpdateNameOperation" -> {
                    bossEventUpdateNameOperationAccessorMethod?.invoke(operation)?.let { name ->
                        bossEventOperationField.set(packet, bossEventUpdateNameOperationComponentField.newInstance((name as Component).transformEmotes(connection.locale())))
                    }
                }
            }
            return packet
        }

        private fun ItemStack.transformItemNameLore(player: Player): ItemStack {
            return copy().apply {
                val locale = player.locale()
                set(DataComponents.ITEM_NAME, get(DataComponents.ITEM_NAME)?.transformEmotes(locale))
                set(DataComponents.LORE, get(DataComponents.LORE)?.let { itemLore ->
                    ItemLore(
                        itemLore.lines.map { Component.empty().setStyle(Style.EMPTY.withItalic(false)).append(it.transformEmotes(locale)) },
                        itemLore.styledLines.map { Component.empty().setStyle(Style.EMPTY).append(it.transformEmotes(locale)) })
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
