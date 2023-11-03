package com.mineinabyss.emojy

import com.mineinabyss.emojy.translator.EmojyLanguage
import com.mineinabyss.idofront.di.DI

val emojy by DI.observe<EmojyContext>()
val emojyConfig by DI.observe<EmojyConfig>()
val templates get() = DI.observe<EmojyTemplates>().getOrNull()?.templates ?: emptySet()
interface EmojyContext {
    val plugin: EmojyPlugin
    val emotes: Set<Emotes.Emote>
    val gifs: Set<Gifs.Gif>
    val languages: Set<EmojyLanguage>
}
