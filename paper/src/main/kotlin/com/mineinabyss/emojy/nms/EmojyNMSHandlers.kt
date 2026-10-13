package com.mineinabyss.emojy.nms

import com.mineinabyss.emojy.EmojyPlugin
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

object EmojyNMSHandlers {

    fun setup(emojy: Plugin): IEmojyNMSHandler {
        // Emojy supports 1.21.11 and up, each module is compiled against the version it handles
        val nmsPackage = when (Bukkit.getMinecraftVersion()) {
            "1.21.11" -> "v1_21_11"
            "26.1", "26.1.1", "26.1.2" -> "v26_1_2"
            "26.2" -> "v26_2"
            "26.3" -> "v26_3"
            else -> throw IllegalStateException("Unsupported server version")
        }
        runCatching {
            return Class.forName("com.mineinabyss.emojy.nms.${nmsPackage}.EmojyNMSHandler").getConstructor(EmojyPlugin::class.java)
                .newInstance(emojy) as IEmojyNMSHandler
        }.onFailure { it.printStackTrace() }

        throw IllegalStateException("Unsupported server version")
    }
}
