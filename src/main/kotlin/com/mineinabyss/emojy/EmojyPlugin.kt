package com.mineinabyss.emojy

import com.comphenix.protocol.ProtocolLib
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.mineinabyss.emojy.packets.EmojyInventoryPacket
import com.mineinabyss.emojy.packets.EmojyTitlePacket
import com.mineinabyss.idofront.config.IdofrontConfig
import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.platforms.Platforms
import com.mineinabyss.idofront.plugin.listeners
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

val protlib: ProtocolLib by lazy { Bukkit.getPluginManager().getPlugin("ProtocolLib") as ProtocolLib }
val protManager: ProtocolManager = ProtocolLibrary.getProtocolManager()
val emojy: EmojyPlugin by lazy { Bukkit.getPluginManager().getPlugin("emojy") as EmojyPlugin }
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
        if (protlib.isEnabled) {
            protManager.addPacketListener(EmojyTitlePacket())
            protManager.addPacketListener(EmojyInventoryPacket())
            //protManager.addPacketListener(EmojyBookPacket())
            //protManager.addPacketListener(EmojySignPacket())
        }

        EmojyCommands()

    }
}
