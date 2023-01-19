package com.mineinabyss.emojy

import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.idofront.config.IdofrontConfig
import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.platforms.Platforms
import com.mineinabyss.idofront.plugin.listeners
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

val emojy: EmojyPlugin by lazy { Bukkit.getPluginManager().getPlugin("Emojy") as EmojyPlugin }
class EmojyPlugin : JavaPlugin(), IEmojyNMSHandler {
    lateinit var config: IdofrontConfig<EmojyConfig>
    override fun onLoad() {
        Platforms.load(this, "mineinabyss")
    }

    override fun onEnable() {
        // NMS version check

        // NMS version check
        if (com.mineinabyss.emojy.nms.EmojyNMSHandlers.getHandler()?.getSupported() != true) {
            logger.severe("This version is not supported! Consider switching versions?")
            server.pluginManager.disablePlugin(this)
            return
        }

        config = config("config") { fromPluginPath(loadDefault = true) }

        EmojyGenerator.generateFontFiles()
        if (emojyConfig.generateResourcePack)
            EmojyGenerator.generateResourcePack()

        listeners(EmojyListener())

        Bukkit.getOnlinePlayers().forEach {
            inject(it)
        }

        EmojyCommands()

    }

    override fun onDisable() {
        Bukkit.getOnlinePlayers().forEach {
            uninject(it)
        }
    }
}
