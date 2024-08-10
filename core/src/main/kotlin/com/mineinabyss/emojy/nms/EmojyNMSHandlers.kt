package com.mineinabyss.emojy.nms

import com.mineinabyss.emojy.EmojyPlugin
import org.bukkit.Bukkit

object EmojyNMSHandlers {

    fun setup(emojy: EmojyPlugin): IEmojyNMSHandler {
        val nmsPackage = when (Bukkit.getMinecraftVersion()) {
            "1.20.5", "1.20.6" -> "v1_20_R4"
            "1.21", "1.21.1" -> "v1_21_R1"
            else -> throw IllegalStateException("Unsupported server version")
        }
        runCatching {
            return Class.forName("com.mineinabyss.emojy.nms.${nmsPackage}.EmojyNMSHandler").getConstructor(EmojyPlugin::class.java)
                .newInstance(emojy) as IEmojyNMSHandler
        }.onFailure { it.printStackTrace() }

        throw IllegalStateException("Unsupported server version")
    }
}
