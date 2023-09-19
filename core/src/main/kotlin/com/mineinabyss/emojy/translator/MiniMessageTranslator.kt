package com.mineinabyss.emojy.translator

import com.mineinabyss.idofront.messaging.logInfo
import com.mineinabyss.idofront.textcomponents.IdofrontTextComponents
import com.mineinabyss.idofront.textcomponents.miniMsg
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.translation.Translator
import java.text.MessageFormat
import java.util.*


abstract class MiniMessageTranslator : Translator {

    protected abstract fun getMiniMessageString(key: String, locale: Locale): String?
    override fun translate(key: String, locale: Locale): MessageFormat? {
        return null
    }

    override fun translate(component: TranslatableComponent, locale: Locale): Component? {
        val mmString = getMiniMessageString(component.key(), locale) ?: return null
        logInfo("Translating ${component.key()} to $mmString")
        val resultingComponent = mmString.miniMsg(EmojyArgumentTag(component.args()).takeIf { component.args().isNotEmpty() } ?: IdofrontTextComponents.globalResolver)
        return when {
            component.children().isEmpty() -> resultingComponent
            else -> resultingComponent.children(component.children())
        }
    }
}
