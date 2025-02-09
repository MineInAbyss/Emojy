package com.mineinabyss.emojy.translator

import com.mineinabyss.emojy.emojy
import com.mineinabyss.idofront.textcomponents.IdofrontTextComponents
import com.mineinabyss.idofront.textcomponents.miniMsg
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.translation.Translator
import java.util.*


class EmojyTranslator : Translator {

    companion object {
        val key = Key.key("emojy", "localization")
    }

    override fun translate(key: String, locale: Locale) = null

    override fun translate(component: TranslatableComponent, locale: Locale): Component? {
        val lang = emojy.languages.firstOrNull { it.locale == locale } ?: emojy.languages.firstOrNull() ?: return null
        val mmString = lang.keys[component.key()] ?: return null
        val tagResolver = component.arguments().takeUnless { it.isEmpty() }?.let(::EmojyArgumentTag) ?: IdofrontTextComponents.globalResolver
        return when {
            component.children().isEmpty() -> mmString.miniMsg(tagResolver)
            else -> mmString.miniMsg(tagResolver).children(component.children())
        }
    }

    override fun name() = key
}
