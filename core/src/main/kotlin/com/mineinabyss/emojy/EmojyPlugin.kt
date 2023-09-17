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
import net.kyori.adventure.translation.TranslationRegistry
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path
import java.text.MessageFormat
import java.util.*
import kotlin.reflect.KProperty

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

        val registry = TranslationRegistry.create(Key.key("emojy", "localization"))
        registry.defaultLocale(Locale.US)
        registry.unregister("mineinabyss.tutorial.welcome.1")
        registry.register("mineinabyss.tutorial.welcome.1", Locale.US, MessageFormat("<yellow>Welcome to <gradient:#ff7043:#ffca28:#ff7043>Mine In Abyss!"))
        GlobalTranslator.translator().addSource(EmojyTranslator(registry))

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
            override val languages: Set<EmojyLanguage> by
                config.supportedLanguages.map { config<EmojyLanguage>(it) {
                    fromPluginPath(relativePath = Path.of("languages"), loadDefault = true)
                } }.toSet()
        }
        DI.add<EmojyContext>(emojyContext)
    }
}

private operator fun <E> Set<E>.getValue(emojyContext: EmojyContext, property: KProperty<*>): Set<EmojyLanguage> {
    return this.map { config<EmojyLanguage>(it) {
        emojy.plugin.fromPluginPath(relativePath = Path.of("languages"), loadDefault = true)
    }.data }.toSet()
}
