@file:OptIn(ExperimentalSerializationApi::class)

package com.mineinabyss.emojy.config

import co.touchlab.kermit.Severity
import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.emojyConfig
import com.mineinabyss.emojy.spaceComponent
import com.mineinabyss.emojy.templates
import com.mineinabyss.idofront.serialization.KeySerializer
import com.mineinabyss.idofront.textcomponents.miniMsg
import kotlinx.serialization.*
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.event.HoverEvent.hoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider

const val PRIVATE_USE_FIRST = 57344
const val SPACE_PERMISSION = "emojy.space"

@Serializable
data class EmojyConfig(
    val defaultPackFormat: Int = 75,
    val defaultNamespace: String = "emotes",
    val defaultFolder: String = "emotes",
    val defaultFont: @Serializable(KeySerializer::class) Key = Key.key("emotes:emotes"),
    val defaultAtlas: @Serializable(KeySerializer::class) Key? = null,
    val defaultHeight: Int = 7,
    val defaultAscent: Int = 7,
    val defaultGifType: Gif.GifType = Gif.GifType.SHADER,
    val generateShader: Boolean = true,
    val spaceFont: @Serializable(KeySerializer::class) Key = Key.key("minecraft:space"),

    val requirePermissions: Boolean = true,
    val supportForceUnicode: Boolean = true,
    val logLevel: Severity = Severity.Debug,
    val emojyList: EmojyList = EmojyList(),
    val supportedLanguages: Set<String> = mutableSetOf("en_us"),
) {

    @Serializable
    data class EmojyList(
        val ignoredEmoteIds: Set<String> = mutableSetOf(),
        val ignoredGifIds: Set<String> = mutableSetOf(),
        val ignoredFonts: Set<@Serializable(KeySerializer::class) Key> = mutableSetOf()
    ) {
        val ignoredEmotes by lazy { emojy.emotes.filter { it.id in ignoredEmoteIds || it.font in ignoredFonts }.toSet() }
        val ignoredGifs by lazy { emojy.gifs.filter { it.id in ignoredGifIds || it.font in ignoredFonts }.toSet() }
    }
}

@Serializable
data class Emotes(val emotes: Set<Emote> = mutableSetOf())
@Serializable
data class Gifs(val gifs: Set<Gif> = mutableSetOf())
