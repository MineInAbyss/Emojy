package com.mineinabyss.emojy

import com.comphenix.protocol.ProtocolLib
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.mineinabyss.idofront.config.IdofrontConfig
import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.platforms.Platforms
import com.mineinabyss.idofront.plugin.listeners
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

val protlib: ProtocolLib by lazy { Bukkit.getPluginManager().getPlugin("ProtocolLib") as ProtocolLib }
val protManager: ProtocolManager = ProtocolLibrary.getProtocolManager()
val emojy: EmojyPlugin by lazy { Bukkit.getPluginManager().getPlugin("Emojy") as EmojyPlugin }
class EmojyPlugin : JavaPlugin() {
    lateinit var config: IdofrontConfig<EmojyConfig>
    override fun onLoad() {
        Platforms.load(this, "mineinabyss")
    }

    override fun onEnable() {
        config = config("config") { fromPluginPath(loadDefault = true) }

        EmojyGenerator.generateFontFiles()
        if (emojyConfig.generateResourcePack)
            EmojyGenerator.generateResourcePack()

        listeners(EmojyListener())

        Bukkit.getOnlinePlayers().forEach(EmojyNMSHandler::inject)

        EmojyCommands()

    }

    override fun onDisable() {
        Bukkit.getOnlinePlayers().forEach(EmojyNMSHandler::uninject)
    }
}
