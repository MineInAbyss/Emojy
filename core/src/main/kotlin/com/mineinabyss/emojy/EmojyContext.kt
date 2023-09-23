package com.mineinabyss.emojy

import com.mineinabyss.emojy.translator.EmojyLanguage
import com.mineinabyss.idofront.di.DI

val emojy by DI.observe<EmojyContext>()
val defaultConfig by DI.observe<EmojyConfig>()
interface EmojyContext {
    val plugin: EmojyPlugin
    val config: EmojyConfig
    val languages: Set<EmojyLanguage>
}
