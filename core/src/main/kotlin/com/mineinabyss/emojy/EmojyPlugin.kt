package com.mineinabyss.emojy

import com.mineinabyss.emojy.nms.EmojyNMSHandlers
import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.di.DI
import com.mineinabyss.idofront.platforms.Platforms
import com.mineinabyss.idofront.plugin.listeners
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class EmojyPlugin : JavaPlugin() {
    override fun onLoad() {
        Platforms.load(this, "mineinabyss")
    }

    override fun onEnable() {
        // NMS version check

        // NMS version check
        if (EmojyNMSHandlers.getHandler()?.supported != true) {
            logger.severe("This version is not supported! Consider switching versions?")
            server.pluginManager.disablePlugin(this)
            return
        }

        createEmojyContext()
        generateFiles()

        listeners(EmojyListener())

        Bukkit.getOnlinePlayers().forEach {
            EmojyNMSHandlers.getHandler()?.inject(it)
        }

        EmojyCommands()

    }

    override fun onDisable() {
        Bukkit.getOnlinePlayers().forEach {
            EmojyNMSHandlers.getHandler()?.uninject(it)
        }
    }
    
    fun generateFiles() {
        EmojyGenerator.generateFontFiles()
        if (emojy.config.generateResourcePack)
            EmojyGenerator.generateResourcePack()
    }
    
    fun createEmojyContext() {
        DI.remove<EmojyContext>()
        val emojyContext = object : EmojyContext {
            override val plugin: EmojyPlugin = this@EmojyPlugin
            override val config: EmojyConfig by config("config") { fromPluginPath(loadDefault = true) }
        }
        DI.add<EmojyContext>(emojyContext)
    }
}
