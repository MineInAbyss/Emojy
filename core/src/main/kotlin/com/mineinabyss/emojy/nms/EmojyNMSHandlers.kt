package com.mineinabyss.emojy.nms

import com.mineinabyss.emojy.EmojyPlugin
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

object EmojyNMSHandlers {

    fun setup(emojy: Plugin): IEmojyNMSHandler {
        // Only the versions whose module is still built belong here, anything else must fail as unsupported
        val nmsPackage = when (Bukkit.getMinecraftVersion()) {
            "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2", "26.3" -> "v1_21_R10"
            else -> throw IllegalStateException("Unsupported server version")
        }
        runCatching {
            return Class.forName("com.mineinabyss.emojy.nms.${nmsPackage}.EmojyNMSHandler").getConstructor(EmojyPlugin::class.java)
                .newInstance(emojy) as IEmojyNMSHandler
        }.onFailure { it.printStackTrace() }

        throw IllegalStateException("Unsupported server version")
    }
}
