package com.mineinabyss.emojy.translator

import kotlinx.serialization.Serializable
import java.util.*

data class EmojyLanguage(val locale: Locale, val keys: Map<String, String>)
@Serializable data class EmojyLanguageMap(val key: String, val value: String)
