package com.mineinabyss.emojy

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mineinabyss.emojy.config.Emote
import com.mineinabyss.emojy.config.Gif
import net.kyori.adventure.key.Key
import java.security.MessageDigest

/**
 * Carries a backend's resolved emotes to the proxy.
 * The proxy opens with its hash of what it last received from that server on [HANDSHAKE_CHANNEL],
 * and the backend only answers on [CHANNEL] when its own payload hashes differently
 */
object EmotePayload {
    const val CHANNEL = "emojy:emotes"
    const val HANDSHAKE_CHANNEL = "emojy:handshake"

    data class Snapshot(val emotes: List<Emote>, val gifs: List<Gif>, val spaceFont: Key)

    fun hash(payload: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(payload).toHexString()

    fun encode(registry: EmoteRegistry): ByteArray = JsonObject().apply {
        addProperty("spaceFont", registry.spaceFont.asString())
        add("emotes", JsonArray().apply { registry.emotes.forEach { add(it.toJson()) } })
        add("gifs", JsonArray().apply { registry.gifs.forEach { add(it.toJson()) } })
    }.toString().encodeToByteArray()

    fun decode(payload: ByteArray): Snapshot {
        val json = JsonParser.parseString(payload.decodeToString()).asJsonObject
        return Snapshot(
            emotes = json.getAsJsonArray("emotes").map { it.asJsonObject.toEmote() },
            gifs = json.getAsJsonArray("gifs").map { it.asJsonObject.toGif() },
            spaceFont = Key.key(json["spaceFont"].asString),
        )
    }

    private fun JsonObject.optional(key: String) = get(key)?.takeUnless(JsonElement::isJsonNull)

    private fun Emote.toJson() = JsonObject().apply {
        addProperty("id", id)
        addProperty("font", font.asString())
        addProperty("atlas", atlas?.asString())
        addProperty("texture", texture.asString())
        addProperty("height", height)
        addProperty("ascent", ascent)
        addProperty("bitmapWidth", bitmapWidth)
        addProperty("bitmapHeight", bitmapHeight)
        add("unicodes", JsonArray().apply { unicodes.forEach(::add) })
    }

    private fun JsonObject.toEmote() = Emote(
        id = this["id"].asString,
        font = Key.key(this["font"].asString),
        atlas = optional("atlas")?.asString?.let(Key::key),
        texture = Key.key(this["texture"].asString),
        height = this["height"].asInt,
        ascent = this["ascent"].asInt,
        bitmapWidth = this["bitmapWidth"].asInt,
        bitmapHeight = this["bitmapHeight"].asInt,
        unicodes = getAsJsonArray("unicodes").map { it.asString },
    )

    private fun Gif.toJson() = JsonObject().apply {
        addProperty("id", id)
        addProperty("frameCount", frameCount)
        addProperty("framePath", framePath.asString())
        addProperty("ascent", ascent)
        addProperty("height", height)
        addProperty("offset", offset)
        addProperty("type", type.name)
    }

    private fun JsonObject.toGif() = Gif(
        id = this["id"].asString,
        frameCount = this["frameCount"].asInt,
        framePath = Key.key(this["framePath"].asString),
        ascent = this["ascent"].asInt,
        height = this["height"].asInt,
        offset = optional("offset")?.asInt,
        type = Gif.GifType.valueOf(this["type"].asString),
    )
}
