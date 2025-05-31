package com.mineinabyss.emojy.nms.v1_21_R3

import com.mineinabyss.emojy.ORIGINAL_ITEM_RENAME_TEXT
import com.mineinabyss.emojy.escapeEmoteIDs
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.transformEmotes
import com.mineinabyss.emojy.unescapeEmoteIds
import com.mineinabyss.idofront.textcomponents.miniMsg
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.papermc.paper.adventure.AdventureComponent
import io.papermc.paper.adventure.PaperAdventure
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPredicate
import net.minecraft.core.component.DataComponents
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.network.RegistryFriendlyByteBuf
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
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.ServerLinks
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.AnvilInventory
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass

class EmojyChannelHandler(val player: Player) : ChannelDuplexHandler() {
    private val serverPlayer: ServerPlayer = (player as CraftPlayer).handle
    private val registryAccess = serverPlayer.server.registryAccess()
    private val protocolInfo = GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registryAccess))

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise?) {
        super.write(ctx, when (msg) {
            is Packet<*> -> transformPacket(msg) ?: return
            is ByteBuf -> runCatching {
                val wrappedBuf = RegistryFriendlyByteBuf(msg, registryAccess)
                transformPacket(protocolInfo.codec().decode(wrappedBuf)) ?: return
            }.getOrDefault(msg)
            else -> msg
        }, promise)
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg !is Packet<*>) return super.channelRead(ctx, msg)
        return super.channelRead(ctx, transformPacket(msg) ?: return)
    }

    inline fun <reified T : Packet<*>> registerTransformer(noinline transformer: (T) -> Packet<*>?) =
        T::class to transformer as (Packet<*>) -> Packet<*>

    inline fun <reified T : Packet<*>> registerReader(noinline reader: (T) -> Unit): Pair<KClass<T>, (Packet<*>) -> Packet<*>> =
        T::class to { packet -> (packet as? T)?.also { reader(it) } ?: packet }

    fun transformPacket(packet: Packet<*>): Packet<*>? {
        val entry = packetTransformers[packet::class] ?: return packet
        return runCatching { entry.invoke(packet) }.onFailure { return packet }.getOrNull()
    }

    private val packetTransformers: Map<KClass<out Packet<*>>, (Packet<*>) -> Packet<*>> = Object2ObjectOpenHashMap(mapOf(
        registerTransformer<ClientboundBundlePacket> {
            ClientboundBundlePacket(it.subPackets().map(::transformPacket) as Iterable<Packet<in ClientGamePacketListener>?>)
        },
        registerTransformer<ClientboundServerLinksPacket> {
            ClientboundServerLinksPacket(it.links.map { ServerLinks.UntrustedEntry(it.type.mapRight { it.transformEmotes() }, it.link) })
        },
        registerTransformer<ClientboundSetScorePacket> {
            ClientboundSetScorePacket(it.owner, it.objectiveName, it.score, it.display.map { it.transformEmotes() }, it.numberFormat)
        },
        registerReader<ClientboundSetObjectivePacket> { packet ->
            setObjectiveDisplayNameField.set(packet, packet.displayName.transformEmotes())
        },
        registerTransformer<ClientboundServerDataPacket> {
            ClientboundServerDataPacket(it.motd.transformEmotes(), it.iconBytes)
        },
        registerTransformer<ClientboundDisguisedChatPacket> {
            ClientboundDisguisedChatPacket(it.message.transformEmotes(true).unescapeEmoteIds(), it.chatType)
        },
        registerTransformer<ClientboundPlayerChatPacket> {
            ClientboundPlayerChatPacket(
                it.sender, it.index, it.signature, it.body,
                (it.unsignedContent ?: PaperAdventure.asVanilla(it.body.content.miniMsg()))?.transformEmotes(true)?.unescapeEmoteIds(),
                it.filterMask, ChatType.bind(it.chatType.chatType.unwrapKey().get(), serverPlayer.registryAccess(), it.chatType.name.transformEmotes(true))
            )
        },
        registerTransformer<ClientboundSystemChatPacket> {
            ClientboundSystemChatPacket(it.content.transformEmotes(true).unescapeEmoteIds(), it.overlay)
        },
        registerTransformer<ClientboundSetTitleTextPacket> {
            ClientboundSetTitleTextPacket(it.text.transformEmotes())
        },
        registerTransformer<ClientboundSetSubtitleTextPacket> {
            ClientboundSetSubtitleTextPacket(it.text.transformEmotes())
        },
        registerTransformer<ClientboundSetActionBarTextPacket> {
            ClientboundSetActionBarTextPacket(it.text.transformEmotes())
        },
        registerTransformer<ClientboundOpenScreenPacket> {
            ClientboundOpenScreenPacket(it.containerId, it.type, it.title.transformEmotes())
        },
        registerTransformer<ClientboundTabListPacket> {
            ClientboundTabListPacket(it.header.transformEmotes(), it.footer.transformEmotes())
        },
        registerTransformer<ClientboundResourcePackPushPacket> {
            ClientboundResourcePackPushPacket(it.id, it.url, it.hash, it.required, it.prompt.map { it.transformEmotes() })
        },
        registerTransformer<ClientboundDisconnectPacket> {
            ClientboundDisconnectPacket(it.reason.transformEmotes())
        },
        registerTransformer<ClientboundSetEntityDataPacket> {
            ClientboundSetEntityDataPacket(it.id, it.packedItems.map {
                when (val value = it.value) {
                    is AdventureComponent -> SynchedEntityData.DataValue(
                        it.id, it.serializer as EntityDataSerializer<AdventureComponent>,
                        AdventureComponent(value.`adventure$component`().transformEmotes())
                    )

                    is Component -> SynchedEntityData.DataValue(it.id, EntityDataSerializers.COMPONENT, value.transformEmotes())
                    is Optional<*> -> when (val comp = value.getOrNull()) {
                        is AdventureComponent -> SynchedEntityData.DataValue(
                            it.id, it.serializer as EntityDataSerializer<Optional<AdventureComponent>>,
                            Optional.of(AdventureComponent(comp.`adventure$component`().transformEmotes()))
                        )

                        is Component -> SynchedEntityData.DataValue(it.id, EntityDataSerializers.OPTIONAL_COMPONENT, Optional.of(comp.transformEmotes()))
                        else -> it
                    }

                    else -> it
                }
            })
        },
        registerTransformer<ClientboundPlayerInfoUpdatePacket> {
            ClientboundPlayerInfoUpdatePacket(it.actions(), it.entries().map { e ->
                ClientboundPlayerInfoUpdatePacket.Entry(
                    e.profileId, e.profile, e.listed, e.latency, e.gameMode,
                    e.displayName?.transformEmotes(), e.showHat, e.listOrder, e.chatSession
                )
            })
        },
        registerTransformer<ClientboundContainerSetSlotPacket> {
            ClientboundContainerSetSlotPacket(it.containerId, it.stateId, it.slot, it.item.transformItemNameLore())
        },
        registerTransformer<ClientboundMerchantOffersPacket> {
            ClientboundMerchantOffersPacket(it.containerId, MerchantOffers().apply {
                addAll(it.offers.map { offer ->
                    MerchantOffer(
                        offer.baseCostA.transform(), offer.costB.map { it.transform() }, offer.result.transformItemNameLore(),
                        offer.uses, offer.maxUses, offer.xp, offer.priceMultiplier, offer.demand, offer.ignoreDiscounts, offer.asBukkit()
                    )
                })
            }, it.villagerLevel, it.villagerXp, it.showProgress(), it.canRestock())
        },
        registerTransformer<ClientboundContainerSetContentPacket> {
            ClientboundContainerSetContentPacket(
                it.containerId, it.stateId, it.items.mapTo(NonNullList.create()) { item ->
                    val inv = player.openInventory.topInventory
                    val bukkit = CraftItemStack.asBukkitCopy(item)

                    if (inv is AnvilInventory && inv.firstItem == bukkit) {
                        item.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getCompound("PublicBukkitValues")?.getString(ORIGINAL_ITEM_RENAME_TEXT.toString())?.let { og ->
                            item.copy().apply { set(DataComponents.CUSTOM_NAME, Component.literal(og)) }
                        } ?: item.transformItemNameLore()
                    } else item.transformItemNameLore()
                }, it.carriedItem
            )
        },
        registerTransformer<ServerboundRenameItemPacket> { packet ->
            ServerboundRenameItemPacket(packet.name.transformEmotes())
        }
    ))

    fun ItemCost.transform() = ItemCost(item, count, components.transformItemNameLore(player), itemStack)

    fun Component.transformEmotes(insert: Boolean = false, locale: Locale? = player.locale()): Component {
        return when {
            this is AdventureComponent -> this.`adventure$component`()
            // Sometimes a NMS component is partially Literal, so ensure entire thing is just one LiteralContent with no extra data
            contents is LiteralContents && style.isEmpty && siblings.isEmpty() ->
                (contents as LiteralContents).text.let { it.takeUnless { "§" in it }?.miniMsg() ?: IEmojyNMSHandler.legacyHandler.deserialize(it) }

            contents is TranslatableContents -> {
                val contents = contents as TranslatableContents
                val args = contents.args.map { (it as? Component)?.transformEmotes(insert, locale) ?: it }.toTypedArray()
                return MutableComponent.create(TranslatableContents(contents.key, contents.fallback, args)).setStyle(style).apply {
                    siblings.map { it.transformEmotes(insert, locale) }.forEach(::append)
                }
            }

            else -> PaperAdventure.asAdventure(this)
        }.transformEmotes(locale, insert).let(PaperAdventure::asVanilla)
    }

    private fun ItemStack.transformItemNameLore(): ItemStack {
        return copy().apply {
            set(DataComponents.ITEM_NAME, get(DataComponents.ITEM_NAME)?.transformEmotes())
            set(DataComponents.LORE, get(DataComponents.LORE)?.let { itemLore ->
                ItemLore(
                    itemLore.lines.map { Component.empty().setStyle(Style.EMPTY.withItalic(false)).append(it.transformEmotes()) },
                    itemLore.styledLines.map { Component.empty().setStyle(Style.EMPTY).append(it.transformEmotes()) })
            })
            get(DataComponents.CUSTOM_DATA)?.copyTag()?.getCompound("PublicBukkitValues")?.getString(ORIGINAL_ITEM_RENAME_TEXT.toString())?.takeIf { it.isNotEmpty() }?.let {
                set(DataComponents.CUSTOM_NAME, PaperAdventure.asVanilla(it.escapeEmoteIDs(player).transformEmotes().unescapeEmoteIds().miniMsg()))
            }
        }
    }

    private fun DataComponentPredicate.transformItemNameLore(player: Player): DataComponentPredicate {
        val map = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, asPatch())

        map.set(DataComponents.ITEM_NAME, map.get(DataComponents.ITEM_NAME)?.transformEmotes())
        map.set(DataComponents.LORE, map.get(DataComponents.LORE)?.let { itemLore ->
            ItemLore(
                itemLore.lines.map { Component.empty().setStyle(Style.EMPTY.withItalic(false)).append(it.transformEmotes()) },
                itemLore.styledLines.map { Component.empty().setStyle(Style.EMPTY).append(it.transformEmotes()) })
        })
        map.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getCompound("PublicBukkitValues")?.getString(ORIGINAL_ITEM_RENAME_TEXT.toString())?.takeIf { it.isNotEmpty() }?.let {
            map.set(DataComponents.CUSTOM_NAME, PaperAdventure.asVanilla(it.escapeEmoteIDs(player).transformEmotes().unescapeEmoteIds().miniMsg()))
        }

        return DataComponentPredicate.allOf(map)
    }

    fun Component.escapeEmoteIDs(): Component {
        return PaperAdventure.asVanilla((PaperAdventure.asAdventure(this)).escapeEmoteIDs(player))
    }

    fun Component.unescapeEmoteIds(): Component {
        return PaperAdventure.asVanilla(PaperAdventure.asAdventure(this).unescapeEmoteIds())
    }

    companion object {
        private val setObjectiveDisplayNameField = ClientboundSetObjectivePacket::class.java.getDeclaredField("displayName").apply { isAccessible = true }
    }
}