package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.EmojyConfig
import com.mineinabyss.emojy.config.EmojyTemplates
import com.mineinabyss.emojy.config.Emotes
import com.mineinabyss.emojy.config.Gifs
import com.mineinabyss.emojy.nms.EmojyNMSHandlers
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.translator.EmojyLanguage
import com.mineinabyss.emojy.translator.EmojyTranslator
import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.di.DI
import com.mineinabyss.idofront.plugin.listeners
import net.kyori.adventure.key.Key
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.io.path.div

class EmojyPlugin : JavaPlugin() {

    override fun onEnable() {
        createEmojyContext()

        // NMS version check
        if (!emojy.handler.supported) {
            logger.severe("This version is not supported! Consider switching versions?")
            server.pluginManager.disablePlugin(this)
            return
        }

        EmojyGenerator.generateResourcePack()

        listeners(EmojyListener())

        server.onlinePlayers.forEach {
            emojy.handler.inject(it)
        }

        EmojyCommands()

    }

    override fun onDisable() {
        server.onlinePlayers.forEach {
            emojy.handler.uninject(it)
        }
    }

    fun createEmojyContext() {
        DI.remove<EmojyConfig>()
        DI.add(config<EmojyConfig>("config", dataFolder.toPath(), EmojyConfig()).getOrLoad())

        DI.remove<EmojyTemplates>()
        DI.add(config<EmojyTemplates>("templates", dataFolder.toPath(), EmojyTemplates()).getOrLoad())

        DI.remove<EmojyContext>()
        DI.add<EmojyContext>(object : EmojyContext {
            override val plugin: EmojyPlugin = this@EmojyPlugin
            override val emotes: Set<Emotes.Emote> = config("emotes", dataFolder.toPath(), Emotes()).getOrLoad().emotes
            override val gifs: Set<Gifs.Gif> = config("gifs", dataFolder.toPath(), Gifs()).getOrLoad().gifs
            override val languages: Set<EmojyLanguage> = emojyConfig.supportedLanguages.map {
                EmojyLanguage(it.split("_").let { l -> Locale(l.first(), l.last().uppercase()) },
                    config<Map<String, String>>(it, dataFolder.toPath() / "languages", mapOf()).getOrLoad())
            }.toSet()
            override val handler: IEmojyNMSHandler = EmojyNMSHandlers.setup()
        })

        GlobalTranslator.translator().sources().filter { it.name() == EmojyTranslator.key }.forEach(GlobalTranslator.translator()::removeSource)
        GlobalTranslator.translator().addSource(EmojyTranslator())
    }
}

