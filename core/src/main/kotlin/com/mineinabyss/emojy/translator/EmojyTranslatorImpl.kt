package com.mineinabyss.emojy.translator

import com.mineinabyss.emojy.replaceEmoteIds
import com.mineinabyss.idofront.messaging.logError
import com.mineinabyss.idofront.messaging.logVal
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer
import net.kyori.adventure.translation.Translator
import net.kyori.examination.ExaminableProperty
import java.text.MessageFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream

object EmojyTranslatorImpl : EmojyTranslator {
    private val NAME: Key = Key.key("emojy", "translator")
    val INSTANCE: EmojyTranslatorImpl = EmojyTranslatorImpl
    val renderer: TranslatableComponentRenderer<Locale> = TranslatableComponentRenderer.usingTranslationSource(this)
    private val sources = Collections.newSetFromMap(ConcurrentHashMap<Translator, Boolean>())


    override fun sources(): Iterable<Translator?> = sources

    override fun addSource(source: Translator): Boolean {
        Objects.requireNonNull(source, "source")
        require(source !== this) { "EmojyTranslationSource" }
        return sources.add(source)
    }

    override fun removeSource(source: Translator): Boolean {
        Objects.requireNonNull(source, "source")
        return sources.remove(source)
    }

    override fun name() = NAME

    override fun translate(key: String, locale: Locale): MessageFormat? {
        Objects.requireNonNull(key, "key")
        Objects.requireNonNull(locale, "locale")
        for (source in sources) source.translate(key, locale)?.let {
            it.logVal("translated: ")
            return it }
        logError("No translatio for $key")
        return null
    }

    override fun translate(component: TranslatableComponent, locale: Locale): Component? {
        Objects.requireNonNull(component, "component")
        Objects.requireNonNull(locale, "locale")
        for (source in sources) source.translate(component, locale)?.let {
            it.serialize().logVal("translated2: ")
            return it.serialize().miniMsg().replaceEmoteIds()
        }
        logError("No translation2 for ${component.key()}")
        return null
    }

    override fun examinableProperties(): Stream<out ExaminableProperty> = Stream.of(ExaminableProperty.of("sources", sources))

}
