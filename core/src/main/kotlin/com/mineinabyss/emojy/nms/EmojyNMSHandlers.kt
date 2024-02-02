package com.mineinabyss.emojy.nms

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mineinabyss.emojy.transform
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import com.mineinabyss.idofront.textcomponents.toPlainText
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object EmojyNMSHandlers {

    private val SUPPORTED_VERSION = arrayOf("v1_19_R1", "v1_19_R2", "v1_19_R3", "v1_20_R1", "v1_20_R2", "v1_20_R3")

    fun setup(): IEmojyNMSHandler {
        SUPPORTED_VERSION.forEach { version ->
            runCatching {
                Class.forName("org.bukkit.craftbukkit.$version.entity.CraftPlayer")
                return Class.forName("com.mineinabyss.emojy.nms.${version}.EmojyNMSHandler").getConstructor()
                    .newInstance() as IEmojyNMSHandler
            }
        }

        throw IllegalStateException("Unsupported server version")
    }

    private val gson = GsonComponentSerializer.gson()
    private val plain = PlainTextComponentSerializer.plainText()
    //TODO toPlainText fixes the anvil issue with tags being escaped
    // It does break all other formatting everywhere else though by removing tags
    // serialize() doesnt but keeps the escaped tag from gson
    // anvil goes like this, inputItem -> readUtf -> renameField -> writeNbt from raw string, which is why it breaks
    fun JsonObject.formatString(player: Player? = null) : String {
        return if (this.has("args") || this.has("text") || this.has("extra") || this.has("translate")) {
            gson.serialize(gson.deserialize(this.toString())/*.toPlainText()*/.serialize().miniMsg().transform(player, true, true))
        } else this.toString()
    }

    fun transformer(player: Player? = null) = { string: String ->
        runCatching {
            val element = JsonParser.parseString(string)
            if (element.isJsonObject) element.asJsonObject.formatString(player)
            else string
        }.getOrNull() ?: string
    }
}
