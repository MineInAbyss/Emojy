package com.mineinabyss.emojy.translator

import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.Translator
import net.kyori.examination.Examinable
import java.util.*

interface EmojyTranslator : Translator, Examinable {

    fun translator() = EmojyTranslatorImpl.INSTANCE
    fun renderer() = EmojyTranslatorImpl.renderer

    fun render(component: Component, locale: Locale) = renderer().render(component, locale)
    fun sources(): Iterable<Translator?>

    fun addSource(source: Translator): Boolean
    fun removeSource(source: Translator): Boolean
}
