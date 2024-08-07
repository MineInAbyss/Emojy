@file:OptIn(ExperimentalSerializationApi::class)

package com.mineinabyss.emojy.config

import com.mineinabyss.idofront.serialization.KeySerializer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import net.kyori.adventure.key.Key

@Serializable(with = EmojyTemplates.Serializer::class)
data class EmojyTemplates(
    val templates: List<EmojyTemplate> = listOf(
        EmojyTemplate(
            "example_template",
            Key.key("example_namespace:example/texture.png"),
            Key.key("example_namespace:example"),
            8,
            8
        )
    )
) {

    @Transient private val ids = templates.map { it.id }

    operator fun contains(template: @UnsafeVariance EmojyTemplate): Boolean = templates.contains(template)
    operator fun contains(id: @UnsafeVariance String): Boolean = ids.contains(id)
    fun forEach(action: (EmojyTemplate) -> Unit) = templates.forEach(action)
    fun filter(filter: (EmojyTemplate) -> Boolean) = templates.filter(filter)
    fun find(predicate: (EmojyTemplate) -> Boolean) = templates.find(predicate)
    operator fun get(id: String) = templates.find { it.id == id }

    class Serializer : InnerSerializer<Map<String, EmojyTemplate>, EmojyTemplates>(
        "emojy:templates",
        MapSerializer(String.serializer(), EmojyTemplate.serializer()),
        { EmojyTemplates(it.map { it.value.copy(id = it.key) }) },
        { it.templates.associateBy { it.id } }
    )
}

@Serializable
data class EmojyTemplate(
    // This is blank by default to avoid marking it as null
    // The Serializer in PackyTemplates will always ensure the id is properly set
    @Transient val id: String = "",
    @EncodeDefault(NEVER) val texture: @Serializable(KeySerializer::class) Key? = null,
    @EncodeDefault(NEVER) val font: @Serializable(KeySerializer::class) Key? = null,
    @EncodeDefault(NEVER) val height: Int? = null,
    @EncodeDefault(NEVER) val ascent: Int? = null,
    @EncodeDefault(NEVER) val bitmapWidth: Int? = null,
    @EncodeDefault(NEVER) val bitmapHeight: Int? = null,
)
