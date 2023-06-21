package com.mineinabyss.emojy

import com.mineinabyss.idofront.di.DI

val emojy by DI.observe<EmojyContext>()
interface EmojyContext {
    val plugin: EmojyPlugin
    val config: EmojyConfig
}
