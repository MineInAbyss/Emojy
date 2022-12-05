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
val handler = EmojyNMSHandler
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
        //if (emojyConfig.enableSignPacketTranslation) listeners(EmojySignTranslator())

        if (protlib.isEnabled) {
            //protManager.addPacketListener(EmojyTitlePacket())
            //protManager.addPacketListener(EmojyInventoryPacket())
        }

        Bukkit.getOnlinePlayers().forEach {
            handler.inject(it)
        }

        EmojyCommands()

    }

    override fun onDisable() {
        Bukkit.getOnlinePlayers().forEach {
            EmojyNMSHandler.uninject(it)
        }
    }
}
