@file:Suppress("unused")

package com.mineinabyss.emojy.nms.v1_20_R4

import com.mineinabyss.emojy.*
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.idofront.messaging.idofrontLogger
import com.mineinabyss.idofront.plugin.listeners
import com.mineinabyss.idofront.textcomponents.serialize
import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.EncoderException
import io.papermc.paper.adventure.AdventureCodecs
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.translation.GlobalTranslator
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import java.util.function.Supplier

class EmojyNMSHandler(emojy: EmojyPlugin) : IEmojyNMSHandler {

    override val locals: MutableSet<Locale> = mutableSetOf()

    override fun addLocaleCodec(locale: Locale) {
        val codecs = (PaperAdventure::class.java.getDeclaredField("LOCALIZED_CODECS").apply { isAccessible = true }
            .get(null) as MutableMap<Locale, Codec<Component>>)

        codecs[locale] = AdventureCodecs.COMPONENT_CODEC.xmap(
            { component -> component },  // decode
            { component -> GlobalTranslator.render(component.transformEmotes(), locale) }  // encode
        )
    }

    init {
        emojy.listeners(EmojyListener())
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

            return component
        }
    }

    override val supported get() = true
}
