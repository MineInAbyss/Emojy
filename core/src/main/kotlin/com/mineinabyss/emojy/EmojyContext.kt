package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.EmojyConfig
import com.mineinabyss.emojy.config.EmojyTemplates
import com.mineinabyss.emojy.config.Emotes
import com.mineinabyss.emojy.config.Gifs
import com.mineinabyss.emojy.nms.EmojyNMSHandlers
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.translator.EmojyLanguage
import com.mineinabyss.idofront.di.DI
import com.mineinabyss.idofront.messaging.ComponentLogger

val emojy by DI.observe<EmojyContext>()
val emojyConfig by DI.observe<EmojyConfig>()
val templates get() = DI.observe<EmojyTemplates>().getOrNull()?.templates ?: emptySet()
interface EmojyContext {
    val plugin: EmojyPlugin
    val emotes: Set<Emotes.Emote>
    val gifs: Set<Gifs.Gif>
    val languages: Set<EmojyLanguage>
    val handler: IEmojyNMSHandler
    val logger: ComponentLogger
}
