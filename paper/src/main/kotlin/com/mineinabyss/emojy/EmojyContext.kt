package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.EmojyConfig
import com.mineinabyss.emojy.config.EmojyTemplates
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.translator.EmojyLanguage
import com.mineinabyss.idofront.messaging.ComponentLogger
import org.bukkit.plugin.Plugin

interface EmojyContext : Plugin, EmoteRegistry {
    val languages: Array<EmojyLanguage>
    val handler: IEmojyNMSHandler
    val logger: ComponentLogger
    val config: EmojyConfig
    val templates: EmojyTemplates
    val generator: EmojyGenerator

    companion object {
        var instance: EmojyContext? = null
    }
}

val emojy get() = EmojyContext.instance ?: error("Emojy not loaded")
