package com.mineinabyss.emojy

import com.mineinabyss.emojy.nms.EmojyNMSHandlers
import com.mineinabyss.emojy.translator.EmojyLanguage
import com.mineinabyss.emojy.translator.EmojyTranslator
import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.di.DI
import com.mineinabyss.idofront.platforms.Platforms
import com.mineinabyss.idofront.plugin.listeners
import net.kyori.adventure.key.Key
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.io.path.div

class EmojyPlugin : JavaPlugin() {
    override fun onLoad() {
        Platforms.load(this, "mineinabyss")
    }

    override fun onEnable() {
        // NMS version check
        if (EmojyNMSHandlers.getHandler()?.supported != true) {
            logger.severe("This version is not supported! Consider switching versions?")
            server.pluginManager.disablePlugin(this)
            return
        }

        createEmojyContext()
        generateFiles()

        listeners(EmojyListener())

        server.onlinePlayers.forEach {
            EmojyNMSHandlers.getHandler()?.inject(it)
        }

        EmojyCommands()

    }

    override fun onDisable() {
        server.onlinePlayers.forEach {
            EmojyNMSHandlers.getHandler()?.uninject(it)
        }
    }

    fun generateFiles() {
        EmojyGenerator.generateFontFiles()
        if (emojy.config.generateResourcePack)
            EmojyGenerator.generateResourcePack()
    }

    fun createEmojyContext() {
        DI.remove<GlobalEmojyConfig>()
        DI.add<GlobalEmojyConfig>(GlobalEmojyConfig())

        DI.remove<EmojyContext>()
        val emojyContext = object : EmojyContext {
            override val plugin: EmojyPlugin = this@EmojyPlugin
            override val config: EmojyConfig by config("config", plugin.dataFolder.toPath(), EmojyConfig())
            override val languages: Set<EmojyLanguage> = config.supportedLanguages.map {
                EmojyLanguage(it.split("_").let { l -> Locale(l.first(), l.last().uppercase()) },
                    config<Map<String, String>>(it, plugin.dataFolder.toPath() / "languages", mapOf()).getOrLoad())
            }.toSet()
        }
        DI.add<EmojyContext>(emojyContext)

        GlobalTranslator.translator().sources().filter { it.name() == Key.key("emojy", "localization") }.forEach(GlobalTranslator.translator()::removeSource)
        GlobalTranslator.translator().addSource(EmojyTranslator())
    }
}

