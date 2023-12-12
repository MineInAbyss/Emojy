package com.mineinabyss.emojy.nms

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mineinabyss.emojy.transform
import io.papermc.paper.text.PaperComponents
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.entity.Player

object EmojyNMSHandlers {

    private val SUPPORTED_VERSION = arrayOf("v1_19_R1", "v1_19_R2", "v1_19_R3", "v1_20_R1", "v1_20_R2", "v1_20_R3")
    private var handler: IEmojyNMSHandler? = null

    fun getHandler(): IEmojyNMSHandler? {
        when {
            handler != null -> return handler
            else -> setup()
        }
        return handler
    }

    private fun setup() {
        if (handler != null) return
        SUPPORTED_VERSION.forEach { version ->
            runCatching {
                handler = Class.forName("com.mineinabyss.emojy.nms.${version}.EmojyNMSHandler").getConstructor()
                    .newInstance() as IEmojyNMSHandler
            }
        }
    }

    private val gson = GsonComponentSerializer.gson()
    fun JsonObject.formatString(player: Player? = null): String {
        return if (this.has("args") || this.has("text") || this.has("extra") || this.has("translate")) {
            gson.serialize(gson.deserializeFromTree(this).transform(player, true))
        } else this.toString()
    }

    fun transformer(player: Player? = null) = { string: String ->
        runCatching {
            val element = JsonParser.parseString(string)
            if (element.isJsonObject) element.asJsonObject.formatString()
            else string
        }.getOrNull() ?: string
    }
}
