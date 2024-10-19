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
        val mmString = emojy.languages.firstOrNull { it.locale == locale }?.keys?.get(component.key()) ?: return null
        val resultingComponent = mmString.miniMsg(EmojyArgumentTag(component.arguments()).takeIf { component.arguments().isNotEmpty() } ?: IdofrontTextComponents.globalResolver)
        return when {
            component.children().isEmpty() -> resultingComponent
            else -> resultingComponent.children(component.children())
        }
    }

    override fun name() = key
}
