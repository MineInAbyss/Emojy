package com.mineinabyss.emojy.nms

import com.mineinabyss.emojy.EmojyPlugin
import org.bukkit.Bukkit

object EmojyNMSHandlers {

    fun setup(emojy: EmojyPlugin): IEmojyNMSHandler {
        val nmsPackage = when (Bukkit.getMinecraftVersion()) {
            "1.21", "1.21.1" -> "v1_21_R1"
            "1.21.2", "1.21.3" -> "v1_21_R2"
            "1.21.4" -> "v1_21_R3"
            "1.21.5" -> "v1_21_R4"
            "1.21.6", "1.21.7", "1.21.8" -> "v1_21_R6"
            else -> throw IllegalStateException("Unsupported server version")
        }
        runCatching {
            return Class.forName("com.mineinabyss.emojy.nms.${nmsPackage}.EmojyNMSHandler").getConstructor(EmojyPlugin::class.java)
                .newInstance(emojy) as IEmojyNMSHandler
        }.onFailure { it.printStackTrace() }

        throw IllegalStateException("Unsupported server version")
    }
}
