package com.mineinabyss.emojy.nms

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mineinabyss.emojy.EmojyPlugin
import com.mineinabyss.emojy.escapeEmoteIDs
import com.mineinabyss.emojy.transform
import com.mineinabyss.emojy.transformEmoteIDs
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object EmojyNMSHandlers {

    private val SUPPORTED_VERSION = arrayOf("v1_20_R4")

    fun setup(emojy: EmojyPlugin): IEmojyNMSHandler {
        SUPPORTED_VERSION.forEach { version ->
            runCatching {
                return Class.forName("com.mineinabyss.emojy.nms.${version}.EmojyNMSHandler").getConstructor(EmojyPlugin::class.java)
                    .newInstance(emojy) as IEmojyNMSHandler
            }.onFailure { it.printStackTrace() }
        }

        throw IllegalStateException("Unsupported server version")
    }
}
