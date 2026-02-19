package com.mineinabyss.emojy.nms.v1_21_R6

import com.mineinabyss.emojy.ORIGINAL_ITEM_RENAME_TEXT
import com.mineinabyss.emojy.escapeEmoteIDs
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.transformEmotes
import com.mineinabyss.emojy.unescapeEmoteIds
import com.mineinabyss.idofront.textcomponents.miniMsg
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.util.ReferenceCountUtil
import io.papermc.paper.adventure.AdventureComponent
import io.papermc.paper.adventure.PaperAdventure
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponentExactPredicate
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.network.chat.numbers.BlankFormat
import net.minecraft.network.chat.numbers.NumberFormat
import net.minecraft.network.chat.numbers.NumberFormatTypes
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.MinecraftServer
import net.minecraft.server.ServerLinks
import net.minecraft.server.dialog.ActionButton
import net.minecraft.server.dialog.CommonButtonData
import net.minecraft.server.dialog.CommonDialogData
import net.minecraft.server.dialog.ConfirmationDialog
import net.minecraft.server.dialog.Dialog
import net.minecraft.server.dialog.DialogListDialog
import net.minecraft.server.dialog.Input
import net.minecraft.server.dialog.MultiActionDialog
import net.minecraft.server.dialog.NoticeDialog
import net.minecraft.server.dialog.ServerLinksDialog
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.dialog.body.ItemBody
import net.minecraft.server.dialog.body.PlainMessage
import net.minecraft.server.dialog.input.BooleanInput
import net.minecraft.server.dialog.input.NumberRangeInput
import net.minecraft.server.dialog.input.SingleOptionInput
import net.minecraft.server.dialog.input.TextInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import net.minecraft.world.scores.Team
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.AnvilInventory
import java.util.*
import kotlin.jvm.optionals.getOrNull

class EmojyChannelHandler(val player: Player) : ChannelDuplexHandler() {

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise?) {
        super.write(ctx, when (msg) {
            is Packet<*> -> transformPacket(msg) ?: return
            is ByteBuf -> {
                val copy = msg.retainedDuplicate()
                val transformed = runCatching {
                    val decodeBuf = RegistryFriendlyByteBuf(msg, registryAccess)
                    val decoded = protocolInfo.codec().decode(decodeBuf)
                    transformPacket(decoded) ?: return run { ReferenceCountUtil.release(msg) }
                }.onSuccess { ReferenceCountUtil.release(msg) }.getOrDefault(msg)

                ReferenceCountUtil.release(copy)
                transformed
            }

            else -> msg
        }, promise)
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg !is Packet<*>) return super.channelRead(ctx, msg)
        return super.channelRead(ctx, transformPacket(msg) ?: return)
    }

    private inline fun <reified T : Packet<*>> registerTransformer(noinline transformer: (T) -> Packet<*>?) {
        packetTransformers[T::class.java] = { packet ->
            if (packet is T) transformer(packet)
            else packet
        }
    }

    private inline fun <reified T : Packet<*>> registerReader(noinline reader: (T) -> Unit) {
        packetTransformers[T::class.java] = { packet ->
            if (packet.javaClass == T::class.java) reader(packet as T)
            packet // Return the original packet unchanged
        }
    }

    private inline fun <reified T : Packet<*>> registerIgnored() {
        packetTransformers[T::class.java] = { packet -> packet }
    }


    fun transformPacket(packet: Packet<*>): Packet<*>? {
        val entry = packetTransformers[packet::class.java] ?: return packet
        return runCatching { entry.invoke(packet) }.onFailure { return packet }.getOrNull()
    }

    private val packetTransformers: MutableMap<Class<out Packet<*>>, (Packet<*>) -> Packet<*>?> = Object2ObjectOpenHashMap()

    init {
        registerIgnored<ClientboundSetEntityMotionPacket>()
        registerIgnored<ClientboundEntityPositionSyncPacket>()
        registerIgnored<ClientboundRotateHeadPacket>()
        registerIgnored<ClientboundMoveEntityPacket>()
        registerIgnored<ClientboundMoveEntityPacket.Pos>()
        registerIgnored<ClientboundMoveEntityPacket.Rot>()
        registerIgnored<ClientboundMoveEntityPacket.PosRot>()
        registerIgnored<ClientboundSetTimePacket>()

        registerTransformer<ClientboundBundlePacket> {
            ClientboundBundlePacket(it.subPackets().map(::transformPacket) as Iterable<Packet<in ClientGamePacketListener>?>)
        }
        registerTransformer<ClientboundServerLinksPacket> {
            ClientboundServerLinksPacket(it.links.map { ServerLinks.UntrustedEntry(it.type.mapRight { it.transformEmotes() }, it.link) })
        }
        registerTransformer<ClientboundSetScorePacket> {
            ClientboundSetScorePacket(it.owner, it.objectiveName, it.score, it.display.map { it.transformEmotes() }, it.numberFormat)
        }
        registerTransformer<ClientboundSetObjectivePacket> { packet ->
            if (packet.method != 0 && packet.method != 2) return@registerTransformer packet
            val displayName = packet.displayName?.transformEmotes() ?: return@registerTransformer packet
            val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess)

            try {
                buf.writeUtf(packet.objectiveName)
                buf.writeByte(packet.method)
                ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, displayName)
                buf.writeEnum(packet.renderType)
                NumberFormatTypes.OPTIONAL_STREAM_CODEC.encode(buf, packet.numberFormat)

                ClientboundSetObjectivePacket.STREAM_CODEC.decode(buf)
            } finally {
                buf.release()
            }
        }
        registerTransformer<ClientboundSetPlayerTeamPacket> { packet ->
            val parameters = packet.parameters.getOrNull() ?: return@registerTransformer packet
            val method = packet.teamAction?.ordinal ?: packet.playerAction?.ordinal ?: EMPTY_TEAM_ACTION
            val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess)

            val shouldHavePlayerList = method == 0 || method == 3 || method == 4
            val shouldHaveParameters = method == 0 || method == 2

            try {
                buf.writeUtf(packet.name)
                buf.writeByte(method)

                if (shouldHaveParameters) {
                    ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, parameters.displayName.transformEmotes())
                    buf.writeByte(parameters.options)
                    Team.Visibility.STREAM_CODEC.encode(buf, parameters.nametagVisibility ?: Team.Visibility.ALWAYS)
                    Team.CollisionRule.STREAM_CODEC.encode(buf, parameters.collisionRule ?: Team.CollisionRule.ALWAYS)
                    buf.writeEnum(parameters.color)
                    ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, parameters.playerPrefix.transformEmotes())
                    ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, parameters.playerSuffix.transformEmotes())
                }

                if (shouldHavePlayerList) buf.writeCollection(packet.players, FriendlyByteBuf::writeUtf)

                // Return the modified packet
                ClientboundSetPlayerTeamPacket.STREAM_CODEC.decode(buf)
            } finally {
                buf.release()
            }
        }
        registerTransformer<ClientboundServerDataPacket> {
            ClientboundServerDataPacket(it.motd.transformEmotes(), it.iconBytes)
        }
        registerTransformer<ClientboundDisguisedChatPacket> {
            ClientboundDisguisedChatPacket(it.message.transformEmotes(true).unescapeEmoteIds(), it.chatType)
        }
        registerTransformer<ClientboundPlayerChatPacket> {
            ClientboundPlayerChatPacket(
                it.globalIndex, it.sender, it.index, it.signature, it.body,
                (it.unsignedContent ?: PaperAdventure.asVanilla(it.body.content.miniMsg()))?.transformEmotes(true)?.unescapeEmoteIds(),
                it.filterMask, ChatType.bind(it.chatType.chatType.unwrapKey().get(), registryAccess, it.chatType.name.transformEmotes(true))
            )
        }
        registerTransformer<ClientboundSystemChatPacket> {
            ClientboundSystemChatPacket(it.content.transformEmotes(true).unescapeEmoteIds(), it.overlay)
        }
        registerTransformer<ClientboundSetTitleTextPacket> {
            ClientboundSetTitleTextPacket(it.text.transformEmotes())
        }
        registerTransformer<ClientboundSetSubtitleTextPacket> {
            ClientboundSetSubtitleTextPacket(it.text.transformEmotes())
        }
        registerTransformer<ClientboundSetActionBarTextPacket> {
            ClientboundSetActionBarTextPacket(it.text.transformEmotes())
        }
        registerTransformer<ClientboundOpenScreenPacket> {
            ClientboundOpenScreenPacket(it.containerId, it.type, it.title.transformEmotes())
        }
        registerTransformer<ClientboundShowDialogPacket> { packet ->
            ClientboundShowDialogPacket(packet.dialog.transform())
        }
        registerTransformer<ClientboundTabListPacket> {
            ClientboundTabListPacket(it.header.transformEmotes(), it.footer.transformEmotes())
        }
        registerTransformer<ClientboundResourcePackPushPacket> {
            ClientboundResourcePackPushPacket(it.id, it.url, it.hash, it.required, it.prompt.map { it.transformEmotes() })
        }
        registerTransformer<ClientboundDisconnectPacket> {
            ClientboundDisconnectPacket(it.reason.transformEmotes())
        }
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
        }
        registerTransformer<ClientboundPlayerInfoUpdatePacket> {
            ClientboundPlayerInfoUpdatePacket(it.actions(), it.entries().map { e ->
                ClientboundPlayerInfoUpdatePacket.Entry(
                    e.profileId, e.profile, e.listed, e.latency, e.gameMode,
                    e.displayName?.transformEmotes(), e.showHat, e.listOrder, e.chatSession
                )
            })
        }
        registerTransformer<ClientboundContainerSetSlotPacket> {
            ClientboundContainerSetSlotPacket(it.containerId, it.stateId, it.slot, it.item.transformItemNameLore())
        }
        registerTransformer<ClientboundMerchantOffersPacket> {
            ClientboundMerchantOffersPacket(it.containerId, MerchantOffers().apply {
                addAll(it.offers.map { offer ->
                    MerchantOffer(
                        offer.baseCostA.transform(), offer.costB.map { it.transform() }, offer.result.transformItemNameLore(),
                        offer.uses, offer.maxUses, offer.xp, offer.priceMultiplier, offer.demand, offer.ignoreDiscounts, offer.asBukkit()
                    )
                })
            }, it.villagerLevel, it.villagerXp, it.showProgress(), it.canRestock())
        }
        registerTransformer<ClientboundContainerSetContentPacket> {
            ClientboundContainerSetContentPacket(
                it.containerId, it.stateId, it.items.mapTo(NonNullList.create()) { item ->
                    val inv = player.openInventory.topInventory
                    val bukkit = CraftItemStack.asBukkitCopy(item)

                    if (inv is AnvilInventory && inv.firstItem == bukkit) {
                        item.get(DataComponents.CUSTOM_DATA)?.unsafe?.getString(ORIGINAL_ITEM_RENAME_TEXT.toString())?.getOrNull()?.let { og ->
                            item.copy().apply { set(DataComponents.CUSTOM_NAME, Component.literal(og)) }
                        } ?: item.transformItemNameLore()
                    } else item.transformItemNameLore()
                }, it.carriedItem.transformItemNameLore()
            )
        }
    }

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
        set(DataComponents.ITEM_NAME, get(DataComponents.ITEM_NAME)?.transformEmotes())
        set(DataComponents.LORE, get(DataComponents.LORE)?.let { itemLore ->
            ItemLore(
                itemLore.lines.map { l -> l.transformEmotes().copy().withStyle { it.withItalic(false) } },
                itemLore.styledLines.map { Component.empty().setStyle(Style.EMPTY).append(it.transformEmotes()) }
            )
        })

        val customName = get(DataComponents.CUSTOM_DATA)?.unsafe?.getString(ORIGINAL_ITEM_RENAME_TEXT.toString())?.getOrNull()?.takeIf { it.isNotEmpty() }
            ?.miniMsg()?.escapeEmoteIDs(player)?.transformEmotes()?.unescapeEmoteIds()?.let(PaperAdventure::asVanilla)
            ?: get(DataComponents.CUSTOM_NAME)?.transformEmotes()
        set(DataComponents.CUSTOM_NAME, customName)

        return this
    }

    private fun DataComponentExactPredicate.transformItemNameLore(player: Player): DataComponentExactPredicate {
        val map = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, asPatch())

        map.set(DataComponents.ITEM_NAME, map.get(DataComponents.ITEM_NAME)?.transformEmotes())
        map.set(DataComponents.LORE, map.get(DataComponents.LORE)?.let { itemLore ->
            ItemLore(
                itemLore.lines.map { l -> l.transformEmotes().copy().withStyle { it.withItalic(false) } },
                itemLore.styledLines.map { Component.empty().setStyle(Style.EMPTY).append(it.transformEmotes()) }
            )
        })

        val customName = map.get(DataComponents.CUSTOM_DATA)?.unsafe?.getString(ORIGINAL_ITEM_RENAME_TEXT.toString())?.getOrNull()?.takeIf { it.isNotEmpty() }
            ?.miniMsg()?.escapeEmoteIDs(player)?.transformEmotes()?.unescapeEmoteIds()?.let(PaperAdventure::asVanilla)
            ?: map.get(DataComponents.CUSTOM_NAME)?.transformEmotes()
        map.set(DataComponents.CUSTOM_NAME, customName)

        return DataComponentExactPredicate.allOf(map)
    }

    private fun Holder<Dialog>.transform(): Holder<Dialog> {
        return Holder.direct(value().transform())
    }

    private fun Dialog.transform(): Dialog {
        val common = common()
        val newCommon = CommonDialogData(
            common.title.transformEmotes(),
            common.externalTitle.map { it.transformEmotes() },
            common.canCloseWithEscape, common.pause,
            common.afterAction, common.body.map(::transform),
            common.inputs.map(::transform)
        )

        return when (this) {
            is ConfirmationDialog -> ConfirmationDialog(newCommon, transform(yesButton), transform(noButton))
            is NoticeDialog -> NoticeDialog(newCommon, transform(action))
            is ServerLinksDialog -> ServerLinksDialog(newCommon, exitAction.map(::transform), columns, buttonWidth)
            is MultiActionDialog -> MultiActionDialog(newCommon, actions.map(::transform), exitAction.map(::transform), columns)
            is DialogListDialog -> DialogListDialog(newCommon, HolderSet.direct(dialogs.map { it.transform() }), exitAction.map(::transform), columns, buttonWidth)
            else -> this
        }
    }

    private fun transform(input: ActionButton): ActionButton {
        val button = CommonButtonData(
            input.button.label.transformEmotes(),
            input.button.tooltip.map { it.transformEmotes() },
            input.button.width
        )
        return ActionButton(button, input.action)
    }

    private fun <T : DialogBody> transform(input: T): T {
        return when (input) {
            is PlainMessage -> PlainMessage(input.contents.transformEmotes(), input.width)
            is ItemBody -> ItemBody(input.item.transformItemNameLore(), input.description.map(::transform), input.showDecorations, input.showTooltip, input.width, input.height)
            else -> this
        } as T
    }

    private fun transform(input: Input): Input {
        val control = when (val control = input.control) {
            is BooleanInput -> BooleanInput(control.label.transformEmotes(), control.initial, control.onTrue, control.onFalse)
            is NumberRangeInput -> NumberRangeInput(control.width, control.label.transformEmotes(), control.labelFormat, control.rangeInfo)
            is SingleOptionInput -> SingleOptionInput(control.width, control.entries, control.label.transformEmotes(), control.labelVisible)
            is TextInput -> TextInput(control.width, control.label.transformEmotes(), control.labelVisible, control.initial, control.maxLength, control.multiline)
            else -> control
        }

        return Input(input.key, control)
    }

    fun Component.escapeEmoteIDs(): Component {
        return PaperAdventure.asVanilla((PaperAdventure.asAdventure(this)).escapeEmoteIDs(player))
    }

    fun Component.unescapeEmoteIds(): Component {
        return PaperAdventure.asVanilla(PaperAdventure.asAdventure(this).unescapeEmoteIds())
    }

    companion object {
        private val bossbarOperationTypeClass: Class<out Enum<*>>? = runCatching {
            Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket\$OperationType")
                .asSubclass(Enum::class.java) // Ensure the class is an Enum type
        }.getOrNull()
        private const val EMPTY_TEAM_ACTION = 2 // Because the int can return a value not tied to any action

        private val registryAccess = MinecraftServer.getServer().registryAccess()
        private val decorator = RegistryFriendlyByteBuf.decorator(registryAccess)
        private val protocolInfo = GameProtocols.CLIENTBOUND_TEMPLATE.bind(decorator)
    }
}