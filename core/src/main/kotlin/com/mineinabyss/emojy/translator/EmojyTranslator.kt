package com.mineinabyss.emojy.translator

import com.mineinabyss.emojy.emojy
import net.kyori.adventure.key.Key
import java.util.*


class EmojyTranslator : MiniMessageTranslator() {
    override fun getMiniMessageString(key: String, locale: Locale): String? {
        return emojy.languages.find { it.locale == locale }?.keys?.get(key)
    }

    override fun name(): Key {
        return Key.key("emojy", "localization")
    }

}
