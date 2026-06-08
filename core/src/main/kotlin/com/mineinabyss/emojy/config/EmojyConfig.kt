@file:OptIn(ExperimentalSerializationApi::class)
@file:UseSerializers(KeySerializer::class)

package com.mineinabyss.emojy.config

import co.touchlab.kermit.Severity
import com.mineinabyss.emojy.emojy
import com.mineinabyss.idofront.serialization.KeySerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.kyori.adventure.key.Key

const val PRIVATE_USE_FIRST = 57344
const val SPACE_PERMISSION = "emojy.space"

@Serializable
data class EmojyConfig(
    val defaultPackFormat: Int = 75,
    val defaultNamespace: String = "emotes",
    val defaultFolder: String = "emotes",
    val defaultFont: Key = Key.key("emotes:emotes"),
    val defaultAtlas: Key? = null,
    val defaultHeight: Int = 7,
    val defaultAscent: Int = 7,
    val defaultGifType: Gif.GifType = Gif.GifType.SHADER,
    val generateShader: Boolean = true,
    val spaceFont: Key = Key.key("minecraft:space"),

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
        val ignoredFonts: Set<Key> = mutableSetOf(),
    ) {
        val ignoredEmotes by lazy { emojy.emotes.filter { it.id in ignoredEmoteIds || it.font in ignoredFonts }.toSet() }
        val ignoredGifs by lazy { emojy.gifs.filter { it.id in ignoredGifIds || it.font in ignoredFonts }.toSet() }
    }
}

@Serializable
data class Emotes(val emotes: Set<EmoteConfig> = mutableSetOf())

@Serializable
data class Gifs(val gifs: Set<GifConfig> = mutableSetOf())
