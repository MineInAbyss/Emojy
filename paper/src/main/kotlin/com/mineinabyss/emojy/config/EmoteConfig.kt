@file:UseSerializers(KeySerializer::class)

package com.mineinabyss.emojy.config

import com.mineinabyss.idofront.serialization.KeySerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider

@Serializable
data class EmoteConfig(
    val id: String,
    @EncodeDefault(NEVER) val template: String? = null,
    @EncodeDefault(NEVER) val font: Key? = null,
    @EncodeDefault(NEVER) val atlas: Key? = null,
    @EncodeDefault(NEVER) val texture: Key? = null,
    @EncodeDefault(NEVER) val height: Int? = null,
    @EncodeDefault(NEVER) val ascent: Int? = null,
    @EncodeDefault(NEVER) val bitmapWidth: Int? = null,
    @EncodeDefault(NEVER) val bitmapHeight: Int? = null,
)

fun Emotes.resolve(templates: EmojyTemplates, config: EmojyConfig): Array<Emote> {
    val emotesPerFont = mutableMapOf<Key, Int>()
    return emotes.map { emote ->
        val template = templates.find { it.id == emote.template }
        // An emote's own values win over the template it otherwise inherits from
        val texture = (emote.texture ?: template?.texture ?: Key.key("${config.defaultNamespace}:${config.defaultFolder}/${emote.id.lowercase()}"))
            .asString().removeSuffix(".png").plus(".png").let(Key::key)
        val font = emote.font ?: template?.font ?: config.defaultFont
        val bitmapWidth = emote.bitmapWidth ?: template?.bitmapWidth ?: 1
        val bitmapHeight = emote.bitmapHeight ?: template?.bitmapHeight ?: 1
        val fontIndex = emotesPerFont[font] ?: 0
        emotesPerFont[font] = fontIndex + 1

        Emote(
            id = emote.id,
            font = font,
            atlas = emote.atlas ?: template?.atlas ?: config.defaultAtlas,
            texture = texture,
            height = emote.height ?: template?.height ?: config.defaultHeight,
            ascent = emote.ascent ?: template?.ascent ?: config.defaultAscent,
            bitmapWidth = bitmapWidth,
            bitmapHeight = bitmapHeight,
            unicodes = Emote.unicodes(fontIndex, bitmapWidth, bitmapHeight),
        )
    }.toTypedArray()
}

val Emote.fontProvider get() = FontProvider.bitMap(texture, height, ascent, unicodes)

fun Emote.appendFont(resourcePack: ResourcePack) {
    val font = resourcePack.font(font)?.toBuilder() ?: Font.font().key(font)
    font.addProvider(fontProvider).build().addTo(resourcePack)
}
