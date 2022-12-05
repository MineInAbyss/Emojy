package com.mineinabyss.emojy

import com.mineinabyss.idofront.config.IdofrontConfig
import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.platforms.Platforms
import com.mineinabyss.idofront.plugin.listeners
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

val emojy: EmojyPlugin by lazy { Bukkit.getPluginManager().getPlugin("Emojy") as EmojyPlugin }
internal val gson = GsonComponentSerializer.gson()
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
