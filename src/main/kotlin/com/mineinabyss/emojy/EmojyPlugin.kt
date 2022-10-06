package com.mineinabyss.emojy

import com.comphenix.protocol.ProtocolLib
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.mineinabyss.idofront.platforms.IdofrontPlatforms
import com.mineinabyss.idofront.plugin.registerEvents
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

val protlib: ProtocolLib by lazy { Bukkit.getPluginManager().getPlugin("ProtocolLib") as ProtocolLib }
val protManager: ProtocolManager = ProtocolLibrary.getProtocolManager()
val emojy: EmojyPlugin by lazy { Bukkit.getPluginManager().getPlugin("emojy") as EmojyPlugin }
class EmojyPlugin : JavaPlugin() {

    override fun onLoad() {
        IdofrontPlatforms.load(this, "mineinabyss")
    }

    override fun onEnable() {
        saveDefaultConfig()
        EmojyGenerator.generateFontFiles()
        if (emojyConfig.generateResourcePack)
            EmojyGenerator.generateResourcePack()

        registerEvents(EmojyListener())
        if (protlib.isEnabled)
            protManager.addPacketListener(EmojyPackets())

        EmojyCommands()

    }
}
