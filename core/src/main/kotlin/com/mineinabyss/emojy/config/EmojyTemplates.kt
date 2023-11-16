@file:OptIn(ExperimentalSerializationApi::class)

package com.mineinabyss.emojy.config

import com.mineinabyss.idofront.serialization.KeySerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key

@Serializable
data class EmojyTemplates(
    val templates: Set<EmojyTemplate> = setOf(
        EmojyTemplate(
            "example_template",
            Key.key("example_namespace:example/texture.png"),
            Key.key("example_namespace:example"),
            8,
            8
        )
    )
)

@Serializable
data class EmojyTemplate(
    val id: String,
    @EncodeDefault(NEVER) val texture: @Serializable(KeySerializer::class) Key? = null,
    @EncodeDefault(NEVER) val font: @Serializable(KeySerializer::class) Key? = null,
    @EncodeDefault(NEVER) val height: Int? = null,
    @EncodeDefault(NEVER) val ascent: Int? = null,
    @EncodeDefault(NEVER) val bitmapWidth: Int? = null,
    @EncodeDefault(NEVER) val bitmapHeight: Int? = null,
)
