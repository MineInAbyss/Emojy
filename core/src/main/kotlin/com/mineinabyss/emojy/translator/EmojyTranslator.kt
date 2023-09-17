package com.mineinabyss.emojy.translator

import com.mineinabyss.emojy.replaceEmoteIds
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationRegistry
import net.kyori.adventure.translation.Translator
import java.util.*

class EmojyTranslator(private val translator: TranslationRegistry) : Translator {
    override fun name() = this.translator.name()
    override fun translate(key: String, locale: Locale) = this.translator.translate(key, locale)

    override fun translate(component: TranslatableComponent, locale: Locale): Component? {
        val miniMessageResult = this.translate(component.key(), locale) ?: return null
        val values = arrayOfNulls<String>(component.args().size)
        component.args().forEachIndexed { index, argumentComponent ->
            values[index] = GlobalTranslator.render(argumentComponent, locale).serialize()
        }
        val resultComponent = miniMessageResult.format(values.filterNotNull().toTypedArray()).miniMsg().replaceEmoteIds()
        val children = resultComponent.children().map { GlobalTranslator.render(it, locale) }
        return GlobalTranslator.render(resultComponent, locale).children(children)
    }

}
