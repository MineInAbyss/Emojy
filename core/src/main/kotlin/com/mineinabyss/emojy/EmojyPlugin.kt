package com.mineinabyss.emojy

import com.mineinabyss.dependencies.*
import com.mineinabyss.emojy.config.*
import com.mineinabyss.emojy.nms.EmojyNMSHandlers
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.translator.EmojyLanguage
import com.mineinabyss.idofront.features.*
import com.mineinabyss.idofront.messaging.ComponentLogger
import com.mineinabyss.idofront.textcomponents.IdofrontTextComponents
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

class EmojyPlugin : JavaPlugin(), EmojyContext, DI {
    override val di: DIContext = DI {
        singlePluginLogger(this@EmojyPlugin)
        single<Plugin> { this@EmojyPlugin }
        single {
            MainCommand(
                names = listOf("emojy"),
                description = "Main command for emojy",
                reloadCommandName = "reload",
            )
        }
    }

    override val logger: ComponentLogger by di.getLazy()
    override val emotes: Array<Emote> get() = scope[EmojyMainModule].get()
    override val gifs: Array<Gif> get() = scope[EmojyMainModule].get()
    override val languages: Array<EmojyLanguage> get() = scope[EmojyMainModule].get()
    override val handler: IEmojyNMSHandler get() = scope[EmojyMainModule].get()
    override val config: EmojyConfig get() = scope[EmojyMainModule].get()
    override val templates: EmojyTemplates get() = scope[EmojyMainModule].get()
    override val generator: EmojyGenerator get() = scope[EmojyMainModule].get()

    override fun onLoad() {
        EmojyContext.instance = this
        IdofrontTextComponents.addResolver(EmojyTags.RESOLVER)
    }

    override fun onEnable() {
        scope.loadAllCatching(EmojyMainModule)
        // NMS version check
        if (!handler.supported) {
            logger.e { "This version is not supported! Consider switching versions?" }
            server.pluginManager.disablePlugin(this)
            return
        }

        scope.load(MainCommandFeature)
    }

    override fun onDisable() {
        di.close()
    }
}

