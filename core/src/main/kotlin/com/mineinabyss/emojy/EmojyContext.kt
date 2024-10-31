package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.*
import com.mineinabyss.emojy.nms.EmojyNMSHandlers
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.translator.EmojyLanguage
import com.mineinabyss.idofront.di.DI
import com.mineinabyss.idofront.messaging.ComponentLogger

val emojy by DI.observe<EmojyContext>()
val emojyConfig by DI.observe<EmojyConfig>()
val templates get() = DI.observe<EmojyTemplates>().getOrNull() ?: EmojyTemplates()
interface EmojyContext {
    val plugin: EmojyPlugin
    val emotes: Array<Emote>
    val gifs: Array<Gif>
    val languages: Array<EmojyLanguage>
    val handler: IEmojyNMSHandler
    val logger: ComponentLogger
}
