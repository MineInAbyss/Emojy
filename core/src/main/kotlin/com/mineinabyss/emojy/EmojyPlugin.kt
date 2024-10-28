package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.*
import com.mineinabyss.emojy.nms.EmojyNMSHandlers
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.translator.EmojyLanguage
import com.mineinabyss.emojy.translator.EmojyTranslator
import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.di.DI
import com.mineinabyss.idofront.messaging.ComponentLogger
import com.mineinabyss.idofront.messaging.injectLogger
import com.mineinabyss.idofront.messaging.observeLogger
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

        EmojyCommands.registerCommands()
    }

    fun createEmojyContext() {
        DI.remove<EmojyConfig>()
        DI.add(config<EmojyConfig>("config", dataPath, EmojyConfig(), onLoad = {
            this@EmojyPlugin.injectLogger(ComponentLogger.forPlugin(this@EmojyPlugin, it.logLevel))
        }).getOrLoad())

        DI.remove<EmojyTemplates>()
        DI.add(config<EmojyTemplates>("templates", dataPath, EmojyTemplates()).getOrLoad())

        DI.remove<EmojyContext>()
        DI.add<EmojyContext>(object : EmojyContext {
            override val plugin: EmojyPlugin = this@EmojyPlugin
            override val emotes: Array<Emote> = config("emotes", dataPath, Emotes()).getOrLoad().emotes.toTypedArray()
            override val gifs: Array<Gif> = config("gifs", dataPath, Gifs()).getOrLoad().gifs.toTypedArray()
            override val languages: Array<EmojyLanguage> = emojyConfig.supportedLanguages.map {
                EmojyLanguage(it.split("_").let { l -> Locale(l.first(), l.last().uppercase()) },
                    config<Map<String, String>>(it, dataPath / "languages", mapOf()).getOrLoad())
            }.toTypedArray()
            override val logger by plugin.observeLogger()
            override val handler: IEmojyNMSHandler = EmojyNMSHandlers.setup(this@EmojyPlugin)
        })

        GlobalTranslator.translator().sources().filter { it.name() == EmojyTranslator.key }.forEach(GlobalTranslator.translator()::removeSource)
        GlobalTranslator.translator().addSource(EmojyTranslator())
    }
}

